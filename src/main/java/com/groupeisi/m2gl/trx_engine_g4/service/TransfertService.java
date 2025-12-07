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
}