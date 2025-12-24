package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.TransfertDto;
import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import com.groupeisi.m2gl.trx_engine_g4.entities.DetailsTransaction;
import com.groupeisi.m2gl.trx_engine_g4.entities.Transfert;
import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import com.groupeisi.m2gl.trx_engine_g4.Repository.CompteRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.TransfertRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.UserRepository;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransfertService {

    private final CompteRepository compteRepository;
    private final TransfertRepository transfertRepository;
    private final UserRepository userRepository;

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

    /**
     * Récupère l'historique des transactions d'un utilisateur par son numéro de téléphone
     */
    public ApiResponse getTransfertsByUser(String phoneNumber) {
        log.info("📋 Récupération de l'historique pour: {}", phoneNumber);
        
        try {
            // 1. Récupérer l'utilisateur
            Optional<User> userOptional = userRepository.findByTelephone(phoneNumber);
            if (userOptional.isEmpty()) {
                return new ApiResponse(
                        "Utilisateur non trouvé",
                        false,
                        404,
                        new ArrayList<>()
                );
            }
            
            User user = userOptional.get();
            if (user.getCompte() == null) {
                return new ApiResponse(
                        "Aucun compte trouvé",
                        false,
                        404,
                        new ArrayList<>()
                );
            }
            
            UUID numCompte = user.getCompte().getNumCompte();
            
            // 2. Récupérer tous les transferts
            List<Transfert> allTransferts = transfertRepository.findAll();
            
            // 3. Filtrer les transferts où l'utilisateur est émetteur ou récepteur
            List<Map<String, Object>> transactions = allTransferts.stream()
                    .filter(t -> t.getDetailsTransaction() != null &&
                            (t.getDetailsTransaction().getCompteEmetteur().equals(numCompte) ||
                             t.getDetailsTransaction().getCompteRecepteur().equals(numCompte)))
                    .sorted((t1, t2) -> t2.getDateTransfert().compareTo(t1.getDateTransfert()))
                    .map(t -> {
                        boolean isDebit = t.getDetailsTransaction().getCompteEmetteur().equals(numCompte);
                        UUID autreCompteId = isDebit 
                            ? t.getDetailsTransaction().getCompteRecepteur() 
                            : t.getDetailsTransaction().getCompteEmetteur();
                        
                        // Récupérer les infos de l'autre compte
                        Optional<Compte> autreCompteOpt = compteRepository.findByNumCompte(autreCompteId);
                        String autreNom = "Inconnu";
                        String autreTelephone = "";
                        
                        if (autreCompteOpt.isPresent()) {
                            Compte autreCompte = autreCompteOpt.get();
                            // Récupérer l'utilisateur associé
                            Optional<User> autreUserOpt = userRepository.findAll().stream()
                                .filter(u -> u.getCompte() != null && u.getCompte().getId().equals(autreCompte.getId()))
                                .findFirst();
                            
                            if (autreUserOpt.isPresent()) {
                                User autreUser = autreUserOpt.get();
                                autreNom = (autreUser.getPrenom() != null && autreUser.getNom() != null)
                                    ? autreUser.getPrenom() + " " + autreUser.getNom()
                                    : autreUser.getNomUtilisateur();
                                autreTelephone = autreUser.getTelephone();
                            }
                        }
                        
                        Map<String, Object> transaction = new HashMap<>();
                        transaction.put("id", t.getId());
                        transaction.put("montant", t.getMontant());
                        transaction.put("date", t.getDateTransfert().toString());
                        transaction.put("isDebit", isDebit);
                        transaction.put("type", isDebit ? "SORTIE" : "ENTREE");
                        transaction.put("autreNom", autreNom);
                        transaction.put("autreTelephone", autreTelephone);
                        return transaction;
                    })
                    .collect(Collectors.toList());
            
            log.info("✅ {} transactions trouvées", transactions.size());
            
            return new ApiResponse(
                    "Historique récupéré avec succès",
                    true,
                    200,
                    transactions
            );
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de l'historique: {}", e.getMessage(), e);
            return new ApiResponse(
                    "Erreur: " + e.getMessage(),
                    false,
                    500,
                    new ArrayList<>()
            );
        }
    }
}