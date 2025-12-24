package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.Repository.CompteRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class CompteService {

    private final CompteRepository compteRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    @Autowired
    public CompteService(CompteRepository compteRepository, UserRepository userRepository,
                         SmsService smsService) {
        this.compteRepository = compteRepository;
        this.userRepository = userRepository;
        this.smsService = smsService;
    }

    // Le temps d'expiration de l'OTP en secondes (ex: 5 minutes)
    private static final long OTP_EXPIRATION_SECONDS = 300;

    /**
     * Crée un compte UNIQUE avec statut 'DISABLE', génère un OTP et l'envoie par SMS.
     * @param user L'utilisateur nouvellement créé.
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse createUniqueCompteAndSendOtp(User user) {
        // 1. Générer le compte
        Compte compte = new Compte();
        compte.setNumCompte(UUID.randomUUID());
        compte.setSolde(0.0f);
        compte.setTypeCompte("CLIENT");
        user.setCompte(compte);
        userRepository.save(user);
        compte.setStatus("DISABLE"); // Statut initial

        // 2. Générer l'OTP et sa durée de validité
        String otp = generateOtp();
        long expiryTime = Instant.now().getEpochSecond() + OTP_EXPIRATION_SECONDS;

        compte.setOtpCode(otp);
        compte.setOtpExpiryTime(expiryTime);

        // 3. Sauvegarder en DB
        compteRepository.save(compte);
        log.info("✅ Compte unique créé pour l'utilisateur ID: {} avec statut DISABLE.", user.getId());

        // 4. Envoyer le code OTP par SMS
        String message = String.format("Votre code d'activation est : %s. Il expire dans 5 minutes.", otp);
        smsService.sendSms(user.getTelephone(), message);
        log.info("📧 Code OTP envoyé au numéro : {}", user.getTelephone());

        return new ApiResponse<>("Compte créé (DISABLE) et OTP envoyé.", true, 201, compte.getNumCompte().toString());
    }

    /**
     * Valide l'OTP et active le compte.
     * @param telephone Le numéro de téléphone de l'utilisateur.
     * @param otp Le code OTP fourni par l'utilisateur.
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse validateOtpAndEnableCompte(String telephone, String otp) {

        // 1️⃣ Récupération du user via le téléphone
        Optional<User> userOpt = userRepository.findByTelephone(telephone);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Utilisateur non trouvé.", false, 404, null);
        }

        User user = userOpt.get();

        // 2️⃣ Récupération du compte via user.getCompte()
        Compte compte = user.getCompte();
        if (compte == null) {
            return new ApiResponse<>("Compte associé non trouvé.", false, 404, null);
        }

        long now = Instant.now().getEpochSecond();

        // 3️⃣ Vérification expiration OTP
        if (compte.getOtpExpiryTime() == null || now > compte.getOtpExpiryTime()) {
            return new ApiResponse<>("Le code OTP a expiré.", false, 400, null);
        }

        // 4️⃣ Vérification OTP
        if (!otp.equals(compte.getOtpCode())) {
            return new ApiResponse<>("Code OTP invalide.", false, 400, null);
        }

        // 5️⃣ Activation du compte
        compte.setStatus("ENABLE");
        compte.setOtpCode(null);
        compte.setOtpExpiryTime(null);
        compte.setSolde(500000);
        compte.setDateCreation(LocalDate.now());

        compteRepository.save(compte);

        log.info("🎉 Compte activé pour l'utilisateur ID: {}", user.getId());

        return new ApiResponse<>("Compte activé avec succès.", true, 200, null);
    }


    /**
     * Génère un code OTP de 6 chiffres.
     */
    private String generateOtp() {
        Random random = new Random();
        // Le format "%06d" assure que le nombre est paddé avec des zéros si moins de 6 chiffres.
        return String.format("%06d", random.nextInt(1000000));
    }

    @Transactional
    public ApiResponse createMerchantCompteAndSendOtp(User user) {

        Compte compte = new Compte();
        compte.setNumCompte(UUID.randomUUID());
        compte.setSolde(0.0f);
        compte.setStatus("DISABLE");
        compte.setTypeCompte("MARCHANT");
        compte.setCodeMarchant(generateCodeMarchant());

        user.setCompte(compte);
        userRepository.save(user);

        // Génération OTP
        String otp = generateOtp();
        long expiryTime = Instant.now().getEpochSecond() + OTP_EXPIRATION_SECONDS;

        compte.setOtpCode(otp);
        compte.setOtpExpiryTime(expiryTime);

        compteRepository.save(compte);

        smsService.sendSms(user.getTelephone(),
                String.format("Votre OTP marchant est : %s (expire dans 5 minutes)", otp));

        log.info("🔥 Compte marchant créé pour user ID {} avec statut DISABLE", user.getId());

        return new ApiResponse<>("Compte marchant créé (DISABLE) et OTP envoyé.",
                true,
                201,
                compte.getId());
    }



    /**
     * Génère un code marchant sur 6 chiffres.
     */
    private int generateCodeMarchant() {
        Random random = new Random();
        return 100000 + random.nextInt(900000); // 6 digits
    }

    @Transactional
    public ApiResponse createMerchantCompte(String telephone) {

        User user = userRepository.findByTelephone(telephone)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur introuvable"));

        // Vérifier s’il a déjà un compte marchand
        /*if (compteRepository.existsByUserIdAndTypeCompte(user.getId(), "MARCHAND")) {
            return new ApiResponse<>(
                    "Compte marchand déjà existant",
                    false,
                    409,
                    null
            );
        }*/

        Compte compte = new Compte();
        compte.setNumCompte(UUID.randomUUID());
        compte.setSolde(0f);
        compte.setTypeCompte("MARCHAND");
        compte.setStatus("DISABLE");
        compte.setCodeMarchant(generateCodeMarchant());
        compte.setDateCreation(LocalDate.now());

        // OTP
        String otp = generateOtp();
        compte.setOtpCode(otp);
        compte.setOtpExpiryTime(
                Instant.now().getEpochSecond() + OTP_EXPIRATION_SECONDS
        );

        compteRepository.save(compte);

        // Lier le compte marchand à l’utilisateur
        user.setCompte(compte);
        userRepository.save(user);

        smsService.sendSms(
                user.getTelephone(),
                "Votre OTP marchand est : " + otp
        );

        return new ApiResponse<>(
                "Compte marchand créé. OTP envoyé.",
                true,
                201,
                compte.getId()
        );
    }

    /**
     * Récupère le compte d'un utilisateur par son numéro de téléphone
     */
    public ApiResponse getCompteByPhone(String telephone) {
        log.info("🔍 Recherche du compte pour le numéro: {}", telephone);
        
        try {
            // 1. Récupérer l'utilisateur par son numéro de téléphone
            Optional<User> userOptional = userRepository.findByTelephone(telephone);
            
            if (userOptional.isEmpty()) {
                log.warn("❌ Utilisateur non trouvé pour le numéro: {}", telephone);
                return new ApiResponse(
                        "Utilisateur non trouvé avec ce numéro de téléphone",
                        false,
                        404,
                        null
                );
            }
            
            User user = userOptional.get();
            
            // 2. Récupérer le compte associé à l'utilisateur
            if (user.getCompte() == null) {
                log.warn("❌ Aucun compte trouvé pour l'utilisateur: {}", user.getNomUtilisateur());
                return new ApiResponse(
                        "Aucun compte trouvé pour cet utilisateur",
                        false,
                        404,
                        null
                );
            }
            
            Compte compte = user.getCompte();
            log.info("✅ Compte trouvé: {} - Solde: {} CFA", compte.getNumCompte(), compte.getSolde());
            
            // 3. Retourner les informations du compte
            return new ApiResponse(
                    "Compte récupéré avec succès",
                    true,
                    200,
                    java.util.Map.of(
                            "numCompte", compte.getNumCompte().toString(),
                            "solde", compte.getSolde(),
                            "typeCompte", compte.getTypeCompte(),
                            "status", compte.getStatus(),
                            "dateCreation", compte.getDateCreation() != null ? compte.getDateCreation().toString() : null
                    )
            );
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du compte: {}", e.getMessage(), e);
            return new ApiResponse(
                    "Erreur lors de la récupération du compte: " + e.getMessage(),
                    false,
                    500,
                    null
            );
        }
    }
}