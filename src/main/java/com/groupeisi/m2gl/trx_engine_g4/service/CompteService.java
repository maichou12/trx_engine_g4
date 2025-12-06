package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.Repository.CompteRepository;
import com.groupeisi.m2gl.trx_engine_g4.Repository.UserRepository;
import com.groupeisi.m2gl.trx_engine_g4.DTOs.CompteDto;
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
    private final ModelMapper modelMapper;
    // Supposons que vous ayez un service SMS pour l'envoi
    private final SmsService smsService;

    @Autowired
    public CompteService(CompteRepository compteRepository, UserRepository userRepository,
                         ModelMapper modelMapper, SmsService smsService) {
        this.compteRepository = compteRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
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
        compte.setTypeCompte("PRINCIPAL");
        compte.setUser(user);
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
        Optional<User> userOpt = userRepository.findByTelephone(telephone);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Utilisateur non trouvé.", false, 404, null);
        }

        // Supposons qu'un utilisateur n'a qu'un seul compte principal pour ce flux
        Optional<Compte> compteOpt = compteRepository.findByUser(userOpt.get());

        if (compteOpt.isEmpty()) {
            return new ApiResponse<>("Compte associé non trouvé.", false, 404, null);
        }

        Compte compte = compteOpt.get();
        long now = Instant.now().getEpochSecond();

        // 1. Vérification de l'expiration
        if (now > compte.getOtpExpiryTime()) {
            // Vous pouvez regénérer l'OTP ici ou demander à l'utilisateur de le faire
            return new ApiResponse<>("Le code OTP a expiré.", false, 400, null);
        }

        // 2. Vérification du code
        if (!compte.getOtpCode().equals(otp)) {
            return new ApiResponse<>("Code OTP invalide.", false, 400, null);
        }

        // 3. Activation du compte
        compte.setStatus("ENABLE");
        // Optionnel : Nettoyer l'OTP après validation
        compte.setOtpCode(null);
        compte.setSolde(500000);
        compte.setDateCreation(LocalDate.now());
        compte.setTypeCompte("CLIENT");
        compte.setOtpExpiryTime(null);
        compteRepository.save(compte);

        log.info("🎉 Compte activé pour l'utilisateur ID: {}", compte.getUser().getId());
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
}