package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.Repository.CompteRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.UserRepository;
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

    private static final long OTP_EXPIRATION_SECONDS = 300;

    @Transactional
    public ApiResponse createUniqueCompteAndSendOtp(User user) {
        Compte compte = new Compte();
        compte.setNumCompte(UUID.randomUUID());
        compte.setSolde(0.0f);
        compte.setTypeCompte("CLIENT");
        compte.setStatus("DISABLE");

        String otp = generateOtp();
        long expiryTime = Instant.now().getEpochSecond() + OTP_EXPIRATION_SECONDS;

        compte.setOtpCode(otp);
        compte.setOtpExpiryTime(expiryTime);

        user.setCompte(compte);
        userRepository.save(user); // Le compte sera sauvegardé via cascade
        log.info("✅ Compte unique cree pour l'utilisateur ID: {} avec statut DISABLE.", user.getId());

        String message = String.format("Votre code d'activation est : %s. Il expire dans 5 minutes.", otp);
        smsService.sendSms(user.getTelephone(), message);
        log.info("📧 Code OTP envoye au numero : {}", user.getTelephone());

        return new ApiResponse("Compte cree (DISABLE) et OTP envoye.", 201, compte.getNumCompte().toString()); 
    }

    @Transactional
    public ApiResponse validateOtpAndEnableCompte(String telephone, String otp) {

        Optional<User> userOpt = userRepository.findByTelephone(telephone);
        if (userOpt.isEmpty()) {
            return new ApiResponse("Utilisateur non trouve.", 404, false); 
        }

        User user = userOpt.get();

        Compte compte = user.getCompte();
        if (compte == null) {
            return new ApiResponse("Compte associe non trouve.", 404, false); 
        }

        // Vérifier si le compte est déjà activé
        if ("ENABLE".equals(compte.getStatus())) {
            return new ApiResponse("Le compte est deja active.", 400, false); 
        }

        // Vérifier si un OTP existe (si le compte a déjà été activé, l'OTP serait null)
        if (compte.getOtpCode() == null || compte.getOtpExpiryTime() == null) {
            return new ApiResponse("Aucun code OTP en attente de validation. Le compte a peut-etre deja ete active.", 400, false); 
        }

        long now = Instant.now().getEpochSecond();

        if (now > compte.getOtpExpiryTime()) {
            return new ApiResponse("Le code OTP a expire.", 400, false); 
        }

        if (!otp.equals(compte.getOtpCode())) {
            return new ApiResponse("Code OTP invalide.", 400, false); 
        }

        compte.setStatus("ENABLE");
        compte.setOtpCode(null);
        compte.setOtpExpiryTime(null);
        compte.setSolde(500000);
        compte.setDateCreation(LocalDate.now());

        compteRepository.save(compte);

        log.info("🎉 Compte active pour l'utilisateur ID: {}", user.getId());

        return new ApiResponse("Compte active avec succes.", 200, null); 
    }

    private String generateOtp() {
        Random random = new Random();
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

        String otp = generateOtp();
        long expiryTime = Instant.now().getEpochSecond() + OTP_EXPIRATION_SECONDS;

        compte.setOtpCode(otp);
        compte.setOtpExpiryTime(expiryTime);

        compteRepository.save(compte);

        smsService.sendSms(user.getTelephone(),
                String.format("Votre OTP marchant est : %s (expire dans 5 minutes)", otp));

        log.info("🔥 Compte marchant cree pour user ID {} avec statut DISABLE", user.getId());

        return new ApiResponse("Compte marchant cree (DISABLE) et OTP envoye.", 201, compte.getId()); 
    }

    private int generateCodeMarchant() {
        Random random = new Random();
        return 100000 + random.nextInt(900000);
    }

    @Transactional
    public ApiResponse createMerchantCompte(String telephone) {

        User user = userRepository.findByTelephone(telephone)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur introuvable"));

        Compte compte = new Compte();
        compte.setNumCompte(UUID.randomUUID());
        compte.setSolde(0f);
        compte.setTypeCompte("MARCHAND");
        compte.setStatus("DISABLE");
        compte.setCodeMarchant(generateCodeMarchant());
        compte.setDateCreation(LocalDate.now());

        String otp = generateOtp();
        compte.setOtpCode(otp);
        compte.setOtpExpiryTime(
                Instant.now().getEpochSecond() + OTP_EXPIRATION_SECONDS
        );

        compteRepository.save(compte);

        smsService.sendSms(
                user.getTelephone(),
                "Votre OTP marchand est : " + otp
        );

        return new ApiResponse("Compte marchand cree. OTP envoye.", 201, compte.getId()); 
    }

    public ApiResponse getCompteByPhone(String telephone) {
        log.info("🔍 Recherche du compte pour le numero: {}", telephone);

        try {
            Optional<User> userOptional = userRepository.findByTelephone(telephone);

            if (userOptional.isEmpty()) {
                log.warn("❌ Utilisateur non trouve pour le numero: {}", telephone);
                return new ApiResponse(
                        "Utilisateur non trouve avec ce numero de telephone",
                        404,
                        false  
                );
            }

            User user = userOptional.get();

            if (user.getCompte() == null) {
                log.warn("❌ Aucun compte trouve pour l'utilisateur: {}", user.getNomUtilisateur());
                return new ApiResponse(
                        "Aucun compte trouve pour cet utilisateur",
                        404,
                        false  
                );
            }

            Compte compte = user.getCompte();
            log.info("✅ Compte trouve: {} - Solde: {} CFA", compte.getNumCompte(), compte.getSolde());

            return new ApiResponse(
                    "Compte recupere avec succes",
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
            log.error("❌ Erreur lors de la recuperation du compte: {}", e.getMessage(), e);
            return new ApiResponse(
                    "Erreur lors de la recuperation du compte: " + e.getMessage(),
                    500,
                    false  
            );
        }
    }
}