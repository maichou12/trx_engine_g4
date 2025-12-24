package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.*;
import com.groupeisi.m2gl.trx_engine_g4.Repository.UserRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.CompteRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.TransfertRepository;
import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import com.groupeisi.m2gl.trx_engine_g4.entities.Transfert;
import com.groupeisi.m2gl.trx_engine_g4.entities.DetailsTransaction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final UserRepository userRepository;
    private final CompteRepository compteRepository;
    private final TransfertRepository transfertRepository;
    
    private static final float MONTANT_MINIMUM = 100.0f;
    
    /**
     * 🔥 1. PAIEMENT CLIENT → MARCHAND
     */
    @Transactional
    public PaymentResponseDto processPaiement(PaiementRequest request) {
        log.info("Paiement: {} → {} | {} FCFA", 
            request.getClientPhone(), request.getMerchantPhone(), request.getAmount());
        
        try {
            if (request.getAmount() < MONTANT_MINIMUM) {
                return PaymentResponseDto.error(
                    String.format("Montant minimum: %.2f FCFA", MONTANT_MINIMUM)
                );
            }
            
            //  Chercher le CLIENT spécifique (pas juste par téléphone)
            List<User> clientUsers = userRepository.findAll().stream()
                    .filter(u -> request.getClientPhone().equals(u.getTelephone()))
                    .filter(u -> u.getCompte() != null && "CLIENT".equals(u.getCompte().getTypeCompte()))
                    .toList();
            
            if (clientUsers.isEmpty()) {
                return PaymentResponseDto.error("Client non trouvé");
            }
            User client = clientUsers.get(0);
            
            Compte compteClient = client.getCompte();
            if (compteClient == null) {
                return PaymentResponseDto.error("Le client n'a pas de compte");
            }
            
            if (!compteClient.isActif()) {
                return PaymentResponseDto.error("Compte client non actif");
            }
            
            //  Chercher le MARCHAND spécifique
            List<User> marchandUsers = userRepository.findAll().stream()
                    .filter(u -> request.getMerchantPhone().equals(u.getTelephone()))
                    .filter(u -> u.getCompte() != null && u.getCompte().isMarchand())
                    .toList();
            
            if (marchandUsers.isEmpty()) {
                return PaymentResponseDto.error("Marchand non trouvé");
            }
            User marchand = marchandUsers.get(0);
            
            Compte compteMarchand = marchand.getCompte();
            if (compteMarchand == null) {
                return PaymentResponseDto.error("Le marchand n'a pas de compte");
            }
            
            if (!compteMarchand.isActif()) {
                return PaymentResponseDto.error("Compte marchand non actif");
            }
            
            float soldeClient = compteClient.getSolde();
            if (soldeClient < request.getAmount()) {
                return PaymentResponseDto.error(
                    String.format("Solde insuffisant. Solde: %.2f FCFA", soldeClient)
                );
            }
            
            compteClient.debiter(request.getAmount());
            compteMarchand.crediter(request.getAmount());
            
            compteRepository.save(compteClient);
            compteRepository.save(compteMarchand);
            
            enregistrerDansTransfertEtDetails(
                compteClient.getNumCompte(),
                compteMarchand.getNumCompte(),
                request.getAmount(),
                request.getMotif()
            );
            
            String transactionId = "PAY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            log.info("Paiement réussi: Client {} → Marchand {}", 
                client.getTelephone(), marchand.getTelephone());
            
            return PaymentResponseDto.success(
                transactionId,
                client.getTelephone(),
                marchand.getTelephone(),
                request.getAmount(),
                compteClient.getSolde(),
                compteMarchand.getSolde(),
                compteMarchand.getCodeMarchant()
            );
            
        } catch (Exception e) {
            log.error("Erreur paiement: {}", e.getMessage(), e);
            return PaymentResponseDto.error(e.getMessage());
        }
    }
    
    /**
     * 🔥 2. TRANSFERT INTERNE - NOUVELLE LOGIQUE 
     */
    @Transactional
    public PaymentResponseDto processTransfertInterne(TransfertInterneRequest request) {
        log.info("Transfert interne: {} | {} FCFA | Direction: {}", 
            request.getTelephone(), request.getAmount(), request.getDirection());
        
        try {
            if (request.getAmount() < MONTANT_MINIMUM) {
                return PaymentResponseDto.error(
                    String.format("Montant minimum: %.2f FCFA", MONTANT_MINIMUM)
                );
            }
            
            if (!"TO_CLIENT".equals(request.getDirection()) && !"TO_MARCHAND".equals(request.getDirection())) {
                return PaymentResponseDto.error("Direction invalide. Options: TO_CLIENT ou TO_MARCHAND");
            }
            
            // 🔥 CORRECTION : Chercher les comptes par type
            List<User> usersAvecCeTelephone = userRepository.findAll().stream()
                    .filter(u -> request.getTelephone().equals(u.getTelephone()))
                    .toList();
            
            if (usersAvecCeTelephone.isEmpty()) {
                return PaymentResponseDto.error("Aucun compte trouvé pour ce numéro");
            }
            
            Compte compteClient = null;
            User userClient = null;
            
            for (User user : usersAvecCeTelephone) {
                if (user.getCompte() != null && "CLIENT".equals(user.getCompte().getTypeCompte())) {
                    compteClient = user.getCompte();
                    userClient = user;
                    break;
                }
            }
            
            if (compteClient == null) {
                return PaymentResponseDto.error("Compte client non trouvé");
            }
            
            Compte compteMarchand = null;
            User userMarchand = null;
            
            for (User user : usersAvecCeTelephone) {
                if (user.getCompte() != null && user.getCompte().isMarchand()) {
                    compteMarchand = user.getCompte();
                    userMarchand = user;
                    break;
                }
            }
            
            if (compteMarchand == null) {
                return PaymentResponseDto.error("Compte marchand non trouvé");
            }
            
            if (!compteClient.isActif() || !compteMarchand.isActif()) {
                return PaymentResponseDto.error("Un des comptes n'est pas actif");
            }
            
            Compte compteSource, compteDestination;
            String transactionId;
            
            if ("TO_CLIENT".equals(request.getDirection())) {
                compteSource = compteMarchand;
                compteDestination = compteClient;
                transactionId = "INT_CLI_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            } else {
                compteSource = compteClient;
                compteDestination = compteMarchand;
                transactionId = "INT_MAR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
            
            float soldeSource = compteSource.getSolde();
            if (soldeSource < request.getAmount()) {
                return PaymentResponseDto.error(
                    String.format("Solde insuffisant. Solde: %.2f FCFA", soldeSource)
                );
            }
            
            compteSource.debiter(request.getAmount());
            compteDestination.crediter(request.getAmount());
            
            compteRepository.save(compteSource);
            compteRepository.save(compteDestination);
            
            enregistrerDansTransfertEtDetails(
                compteSource.getNumCompte(),
                compteDestination.getNumCompte(),
                request.getAmount(),
                request.getMotif() != null ? request.getMotif() : "Transfert interne"
            );
            
            log.info("Transfert interne réussi: {} → {} | {} FCFA", 
                compteSource.getTypeCompte(), 
                compteDestination.getTypeCompte(), 
                request.getAmount());
            
            return PaymentResponseDto.success(
                transactionId,
                request.getTelephone(),
                request.getTelephone(),
                request.getAmount(),
                compteClient.getSolde(),
                compteMarchand.getSolde(),
                compteMarchand.getCodeMarchant()
            );
            
        } catch (Exception e) {
            log.error("Erreur transfert interne: {}", e.getMessage());
            return PaymentResponseDto.error(e.getMessage());
        }
    }
    
    /**
     * 🔥 3. CRÉER UN COMPTE MARCHAND SÉPARÉ
     */
    @Transactional
    public PaymentResponseDto creerCompteMarchandSepare(ActiverMarchantRequest request) {
        log.info("Création compte marchand séparé pour: {}", request.getTelephone());
        
        try {
            // Vérifier que l'utilisateur a un compte CLIENT
            List<User> usersAvecCeTelephone = userRepository.findAll().stream()
                    .filter(u -> request.getTelephone().equals(u.getTelephone()))
                    .toList();
            
            User userClient = null;
            for (User user : usersAvecCeTelephone) {
                if (user.getCompte() != null && "CLIENT".equals(user.getCompte().getTypeCompte())) {
                    userClient = user;
                    break;
                }
            }
            
            if (userClient == null) {
                return PaymentResponseDto.error("Vous devez d'abord avoir un compte client");
            }
            
            // Vérifier qu'il n'a pas déjà un compte MARCHAND
            for (User user : usersAvecCeTelephone) {
                if (user.getCompte() != null && user.getCompte().isMarchand()) {
                    return PaymentResponseDto.error("Vous avez déjà un compte marchand");
                }
            }
            
            if (request.getCodeMarchant() == null) {
                return PaymentResponseDto.error("Code marchant requis");
            }
            
            if (request.getCodeMarchant() < 100000 || request.getCodeMarchant() > 999999) {
                return PaymentResponseDto.error("Code marchant invalide. Doit avoir 6 chiffres (100000-999999)");
            }
            
            boolean codeDejaUtilise = compteRepository.findByCodeMarchant(request.getCodeMarchant()).isPresent();
            if (codeDejaUtilise) {
                return PaymentResponseDto.error("Ce code marchant est déjà utilisé");
            }
            
            // 🔥 CRÉER LE COMPTE MARCHAND
            Compte nouveauCompteMarchand = new Compte();
            nouveauCompteMarchand.setNumCompte(UUID.randomUUID());
            nouveauCompteMarchand.setSolde(0.0f);
            nouveauCompteMarchand.setTypeCompte("MARCHANT");
            nouveauCompteMarchand.setCodeMarchant(request.getCodeMarchant());
            nouveauCompteMarchand.setDateCreation(java.time.LocalDate.now());
            nouveauCompteMarchand.setStatus("ACTIVE");
            
            compteRepository.save(nouveauCompteMarchand);
            
            // 🔥 CRÉER UN NOUVEAU USER pour le compte marchand
            User nouveauUserMarchand = new User();
            nouveauUserMarchand.setNom(userClient.getNom());
            nouveauUserMarchand.setPrenom(userClient.getPrenom());
            nouveauUserMarchand.setTelephone(userClient.getTelephone());
            nouveauUserMarchand.setNomUtilisateur(userClient.getNomUtilisateur() + "_marchand");
            nouveauUserMarchand.setRoleName("MARCHAND");
            nouveauUserMarchand.setKeycloakId(userClient.getKeycloakId() + "_marchand");
            nouveauUserMarchand.setNin(userClient.getNin());
            
            nouveauUserMarchand.setCompte(nouveauCompteMarchand);
            
            userRepository.save(nouveauUserMarchand);
            
            String transactionId = "CRE_MAR_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            
            log.info("Compte marchand séparé créé pour: {} | Même téléphone, compte séparé", 
                request.getTelephone());
            
            PaymentResponseDto response = new PaymentResponseDto();
            response.setSuccess(true);
            response.setMessage("Compte marchand créé avec succès (séparé du compte client)");
            response.setTransactionId(transactionId);
            response.setReference("CRE_" + transactionId);
            response.setClientPhone(userClient.getTelephone());
            response.setMerchantPhone(userClient.getTelephone());
            response.setMerchantCode(request.getCodeMarchant());
            response.setTimestamp(LocalDateTime.now());
            response.setStatus("COMPLETED");
            response.setAmount(0);
            response.setNewClientBalance(userClient.getCompte().getSolde());
            response.setNewMerchantBalance(0.0f);
            
            return response;
            
        } catch (Exception e) {
            log.error("Erreur DÉTAILLÉE création compte marchand séparé: {}", e.getMessage(), e);
            return PaymentResponseDto.error("Échec création: " + e.getMessage());
        }
    }
    
    /**
     * 🔥 4. CONSULTER SOLDE D'UN TYPE DE COMPTE
     */
    public float getSolde(String telephone, String typeCompte) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> telephone.equals(u.getTelephone()))
                .toList();
        
        for (User user : users) {
            if (user.getCompte() != null && typeCompte.equals(user.getCompte().getTypeCompte())) {
                return user.getCompte().getSolde();
            }
        }
        
        throw new RuntimeException("Compte " + typeCompte + " non trouvé pour " + telephone);
    }
    
    /**
     * 🔥 5. CONSULTER SOLDE TOTAL (CLIENT + MARCHAND)
     */
    public float getSoldeTotal(String telephone) {
        float total = 0.0f;
        
        List<User> users = userRepository.findAll().stream()
                .filter(u -> telephone.equals(u.getTelephone()))
                .toList();
        
        for (User user : users) {
            if (user.getCompte() != null) {
                total += user.getCompte().getSolde();
            }
        }
        
        return total;
    }
    
    /**
     * 🔥 6. VÉRIFIER SI UN UTILISATEUR A UN COMPTE MARCHAND
     */
    public boolean aCompteMarchand(String telephone) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> telephone.equals(u.getTelephone()))
                .toList();
        
        for (User user : users) {
            if (user.getCompte() != null && user.getCompte().isMarchand()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🔥 7. DÉSACTIVER COMPTE MARCHAND
     */
    @Transactional
    public PaymentResponseDto desactiverCompteMarchand(String telephone) {
        try {
            List<User> users = userRepository.findAll().stream()
                    .filter(u -> telephone.equals(u.getTelephone()))
                    .toList();
            
            Compte compteMarchand = null;
            User userMarchand = null;
            
            for (User user : users) {
                if (user.getCompte() != null && user.getCompte().isMarchand()) {
                    compteMarchand = user.getCompte();
                    userMarchand = user;
                    break;
                }
            }
            
            if (compteMarchand == null) {
                return PaymentResponseDto.error("Vous n'avez pas de compte marchand");
            }
            
            if (compteMarchand.getSolde() > 0) {
                return PaymentResponseDto.error("Vous devez transférer votre solde vers votre compte client avant de désactiver");
            }
            
            compteMarchand.setStatus("DESACTIVATED");
            compteRepository.save(compteMarchand);
            
            log.info("Compte marchand désactivé pour: {}", telephone);
            
            PaymentResponseDto response = new PaymentResponseDto();
            response.setSuccess(true);
            response.setMessage("Compte marchand désactivé avec succès");
            response.setTransactionId("DESACT_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            response.setTimestamp(LocalDateTime.now());
            response.setStatus("COMPLETED");
            
            return response;
            
        } catch (Exception e) {
            log.error("Erreur désactivation: {}", e.getMessage());
            return PaymentResponseDto.error("Échec désactivation: " + e.getMessage());
        }
    }
    
    /**
     * Méthode pour enregistrer les transactions
     */
    private void enregistrerDansTransfertEtDetails(
            UUID compteEmetteurUuid, 
            UUID compteRecepteurUuid,
            float montant, 
            String motif) {
        
        try {
            DetailsTransaction details = new DetailsTransaction();
            details.setCompteEmetteur(compteEmetteurUuid);
            details.setCompteRecepteur(compteRecepteurUuid);
            
            Transfert transfert = new Transfert();
            transfert.setMontant(montant);
            transfert.setDateTransfert(LocalDateTime.now());
            transfert.setDetailsTransaction(details);
            
            transfertRepository.save(transfert);
            
            log.info("✅ Transaction enregistrée: {} FCFA", montant);
        } catch (Exception e) {
            log.error("⚠️ Impossible d'enregistrer la transaction: {}", e.getMessage());
        }
    }
}