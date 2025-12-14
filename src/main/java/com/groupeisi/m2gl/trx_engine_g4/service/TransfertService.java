package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.TransfertDto;
import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import com.groupeisi.m2gl.trx_engine_g4.entities.DetailsTransaction;
import com.groupeisi.m2gl.trx_engine_g4.entities.Transfert;
import com.groupeisi.m2gl.trx_engine_g4.Repository.CompteRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.TransfertRepository;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse; // Importez ApiResponse
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransfertService {

    private final CompteRepository compteRepository;
    private final TransfertRepository transfertRepository;

    @Transactional
    public ApiResponse<Transfert> effectuerTransfert(TransfertDto transfertDto) {
        // 1. Extraction des données simplifiée
        float montant = transfertDto.getMontant();
        UUID uuidEmetteur = transfertDto.getCompteEmetteur();
        UUID uuidRecepteur = transfertDto.getCompteRecepteur();

        // 2. Validations de base
        if (uuidEmetteur.equals(uuidRecepteur)) {
            throw new IllegalArgumentException("Impossible d'effectuer un transfert vers le même compte.");
        }

        // 3. Récupération des comptes
        // Les exceptions sont levées ici (EntityNotFoundException, etc.)
        Compte emetteur = getCompteOrThrow(uuidEmetteur, "émetteur");
        Compte recepteur = getCompteOrThrow(uuidRecepteur, "récepteur");

        // 4. Validations métier
        if ("BLOQUE".equalsIgnoreCase(emetteur.getStatus()) || "BLOQUE".equalsIgnoreCase(recepteur.getStatus())) {
            throw new IllegalStateException("L'un des comptes est bloqué ou inactif.");
        }
        if (emetteur.getSolde() < montant) {
            throw new IllegalArgumentException("Solde insuffisant pour effectuer ce transfert. Solde actuel: " + emetteur.getSolde());
        }

        // 5. Opération de transfert
        emetteur.setSolde(emetteur.getSolde() - montant);
        recepteur.setSolde(recepteur.getSolde() + montant);

        // Sauvegarde des comptes mis à jour
        compteRepository.save(emetteur);
        compteRepository.save(recepteur);

        // 6. Enregistrement de l'historique
        Transfert nouveauTransfert = enregistrerTransfert(transfertDto);

        // 7. Retourne l'ApiResponse formaté
        return new ApiResponse<>(
                "La transaction a été effectuée avec succès.",
                HttpStatus.CREATED.value(),
                null
        );
    }

    private Compte getCompteOrThrow(UUID numCompte, String type) {
        return compteRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new EntityNotFoundException("Compte " + type + " introuvable : " + numCompte));
    }

    private Transfert enregistrerTransfert(TransfertDto dto) {
        DetailsTransaction details = new DetailsTransaction();
        details.setCompteEmetteur(dto.getCompteEmetteur());
        details.setCompteRecepteur(dto.getCompteRecepteur());
        Transfert transfert = new Transfert();
        transfert.setMontant(dto.getMontant());
        transfert.setDateTransfert(LocalDateTime.now());
        transfert.setDetailsTransaction(details);

        return transfertRepository.save(transfert);
    }

//    @Transactional
//    public ApiResponse<Transfert> payerMarchand(UUID idClient, UUID idMarchand, float montant) {
//
//        // Vérifie qu'un client ne peut pas se payer lui-même
//        if (idClient.equals(idMarchand)) {
//            throw new IllegalArgumentException("Le client et le marchand ne peuvent pas être le même compte.");
//        }
//
//        // Récupère les comptes ou lance une exception s'ils n'existent pas
//        Compte client = getCompteOrThrow(idClient, "client");
//        Compte marchand = getCompteOrThrow(idMarchand, "marchand");
//
//        // Vérifie que le compte client n'est pas bloqué
//        if ("BLOQUE".equalsIgnoreCase(client.getStatus())) {
//            throw new IllegalStateException("Le compte client est bloqué.");
//        }
//
//        // Vérifie que le compte marchand n'est pas bloqué
//        if ("BLOQUE".equalsIgnoreCase(marchand.getStatus())) {
//            throw new IllegalStateException("Le compte marchand est bloqué.");
//        }
//
//        // Vérifie que le solde du client est suffisant pour le paiement
//        if (client.getSolde() < montant) {
//            throw new IllegalArgumentException("Solde insuffisant. Solde actuel : " + client.getSolde());
//        }
//
//        // Débiter le client
//        client.setSolde(client.getSolde() - montant);
//
//        // Créditer le marchand
//        marchand.setSolde(marchand.getSolde() + montant);
//
//        // Sauvegarde des nouveaux soldes dans la base
//        compteRepository.save(client);
//        compteRepository.save(marchand);
//
//        // Préparation du DTO pour historiser l'opération
//        TransfertDto dto = new TransfertDto();
//        dto.setMontant(montant);
//        dto.setCompteEmetteur(idClient);
//        dto.setCompteRecepteur(idMarchand);
//
//        // Enregistrement de l'opération dans un historique
//        Transfert historique = enregistrerTransfert(dto);
//
//        // Retour d'une réponse API standardisée
//        return new ApiResponse<>(
//                "Paiement effectué avec succès (client → marchand).",
//                201,
//                historique
//        );
//    }

    @Transactional
    public ApiResponse<Transfert> payerMarchand(UUID idClient, UUID idMarchand, float montant) {

        // 1️ Vérifier que client ≠ marchand
        if (idClient.equals(idMarchand)) {
            return new ApiResponse<>(
                    "Le client et le marchand ne peuvent pas être le même compte.",
                    400,
                    null
            );
        }

        // Récupération des comptes (SANS exception)
        Compte client = compteRepository.findByNumCompte(idClient).orElse(null);
        Compte marchand = compteRepository.findByNumCompte(idMarchand).orElse(null);

        if (client == null) {
            return new ApiResponse<>(
                    "Compte client introuvable.",
                    404,
                    null
            );
        }

        if (marchand == null) {
            return new ApiResponse<>(
                    "Compte marchand introuvable.",
                    404,
                    null
            );
        }

        //  Vérifications métier
        if ("BLOQUE".equalsIgnoreCase(client.getStatus())) {
            return new ApiResponse<>(
                    "Le compte client est bloqué.",
                    403,
                    null
            );
        }

        if ("BLOQUE".equalsIgnoreCase(marchand.getStatus())) {
            return new ApiResponse<>(
                    "Le compte marchand est bloqué.",
                    403,
                    null
            );
        }

        if (client.getSolde() < montant) {
            return new ApiResponse<>(
                    "Solde insuffisant. Solde actuel : " + client.getSolde(),
                    400,
                    null
            );
        }

        //  Débit / Crédit
        client.setSolde(client.getSolde() - montant);
        marchand.setSolde(marchand.getSolde() + montant);

        compteRepository.save(client);
        compteRepository.save(marchand);

        //  Historique
        TransfertDto dto = new TransfertDto();
        dto.setMontant(montant);
        dto.setCompteEmetteur(idClient);
        dto.setCompteRecepteur(idMarchand);

        Transfert historique = enregistrerTransfert(dto);

        // Réponse succès
        return new ApiResponse<>(
                "Paiement effectué avec succès (client → marchand).",
                201,
                historique
        );
    }




}