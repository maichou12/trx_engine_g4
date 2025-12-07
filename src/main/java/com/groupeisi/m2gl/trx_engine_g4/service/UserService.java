package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.Repository.UserRepository;
import com.groupeisi.m2gl.trx_engine_g4.DTOs.UserDto;
import com.groupeisi.m2gl.trx_engine_g4.request.RegisterRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final KeycloakService keycloakService;
    private final UserRepository userRepository;
    private final org.modelmapper.ModelMapper modelMapper;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private final CompteService compteService;

    @Autowired
    public UserService(PlatformTransactionManager transactionManager, KeycloakService keycloakService,
                       UserRepository userRepository, org.modelmapper.ModelMapper modelMapper,
                       CompteService compteService) {
        this.keycloakService = keycloakService;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.compteService = compteService;
    }

    /**
     * Fonction d'inscription simplifiée type Wave
     */
    @Transactional // La transaction DB va gérer le rollback local, nous gérons le rollback Keycloak.
    public ApiResponse registerUser(RegisterRequest registerRequest) {
        log.info("➡️ Début inscription utilisateur : {}", registerRequest.getNomUtilisateur());

        // Variable pour stocker l'ID Keycloak en cas de besoin de suppression
        String keycloakUserId = null;

        try {
            // 1. Validation des données de base
            ApiResponse validationResponse = validateRegistrationData(registerRequest);
            if (!validationResponse.isSuccess()) {
                return validationResponse;
            }

            // 2. Vérification du téléphone (DB locale)
            if (phoneExists(registerRequest.getTelephone())) {
                return new ApiResponse<>("Ce numéro de téléphone est déjà enregistré", false, 409, null);
            }

            // 3. Vérification du nom d'utilisateur (Keycloak)
            String username = registerRequest.getNomUtilisateur();
            log.info("🔍 Vérification disponibilité username : {}", username);

            ApiResponse<String> usernameCheck = keycloakService.usernameExists(username);

            if (!usernameCheck.isSuccess() && usernameCheck.getStatusCode() == 409) {
                log.warn("❌ Username '{}' déjà pris.", username);
                return new ApiResponse<>("Le nom d'utilisateur '" + username + "' est déjà pris ou indisponible.", false, 409, null);
            }

            if (!usernameCheck.isSuccess() && usernameCheck.getStatusCode() >= 500) {
                log.error("💥 ERREUR Keycloak - Problème d'authentification/serveur : Code {}", usernameCheck.getStatusCode());
                return new ApiResponse<>("Erreur de connexion au serveur d'identité Keycloak (vérifiez les logs KeycloakService).", false, usernameCheck.getStatusCode(), null);
            }

            // 4. Création DTO
            UserDto userDTO = createUserDtoFromRequest(registerRequest, username);

            // 5. Création dans Keycloak (Étape critique 1)
            keycloakUserId = keycloakService.createUser(userDTO);
            log.info("   ✅ Utilisateur créé dans Keycloak ID = {}", keycloakUserId);

            // 6. Attribution rôle
            String roleName = registerRequest.getRoleName() != null ? registerRequest.getRoleName() : "user";
            ApiResponse roleResponse = keycloakService.addRoleToUser(keycloakUserId, roleName);

            if (!roleResponse.isSuccess()) {
                // Si l'attribution du rôle échoue, on doit aussi rollback Keycloak
                log.info(" -------------- Utilisateur créé dans Keycloak ID = {} -----------", roleResponse);
                throw new RuntimeException("Erreur lors de l'attribution du rôle: " + roleResponse.getMessage());
            }

            // 7. Sauvegarde en DB locale (Étape critique 2 - L'échec précédent était ici)
            userDTO.setRoleName(roleName);
            ApiResponse<User> saveResponse = saveUserInDatabase(userDTO, keycloakUserId);
            if (!saveResponse.isSuccess()) {
                throw new RuntimeException("Erreur de validation ou de persistance DB: " + saveResponse.getMessage());
            }

            User savedUser = (User) saveResponse.getData();
            ApiResponse compteResponse = compteService.createUniqueCompteAndSendOtp(savedUser);

            if (!compteResponse.isSuccess()) {
                throw new RuntimeException("Erreur lors de la création du compte/OTP: " + compteResponse.getMessage());
            }
            return new ApiResponse<>(
                    "Inscription réussie. Veuillez valider votre compte en utilisant le code OTP envoyé par SMS.",
                    true,
                    201,
                    Map.of(
                            "username", username,
                            "userId", keycloakUserId,
                            "numCompte", compteResponse.getData()
                    )
            );

        } catch (Exception e) {
            log.error("💥 ERREUR lors de l'inscription : {}", e.getMessage(), e);

            // 💥 LOGIQUE DE COMPENSATION (ROLLBACK KEYCLOAK)
            if (keycloakUserId != null) {
                log.warn("➡️ COMPENSATION : L'inscription locale a échoué. Suppression de l'utilisateur Keycloak ID : {}", keycloakUserId);
                // On ignore le résultat de la suppression, car la vraie erreur est celle d'origine.
                keycloakService.deleteUser(keycloakUserId);
                log.warn("   ✅ Compensation Keycloak effectuée.");
            }

            // Si l'erreur provient de la validation JPA (contraintes sur NIN ou téléphone, etc.)
            if (e instanceof ConstraintViolationException) {
                return new ApiResponse<>("Validation DB locale échouée: Assurez-vous que le NIN est présent et le téléphone valide.", false, 400, null);
            }

            // Gestion des erreurs Keycloak non catchées ou autres erreurs inattendues
            return new ApiResponse<>("Erreur technique: " + e.getMessage(), false, 500, null);
        }
    }
    /**
     * Validation des données d'inscription
     */
    private ApiResponse validateRegistrationData(RegisterRequest request) {
        // Vérification du téléphone
        if (request.getTelephone() == null || request.getTelephone().trim().isEmpty()) {
            return new ApiResponse<>("Le numéro de téléphone est obligatoire", false, 400, null);
        }

        // Validation format téléphone (E.164)
        if (!isValidPhoneNumber(request.getTelephone())) {
            return new ApiResponse<>(
                    "Format de téléphone invalide. Utilisez le format international: +221XXXXXXXXX",
                    false, 400, null
            );
        }

        // Vérification prénom
        if (request.getPrenom() == null || request.getPrenom().trim().isEmpty()) {
            return new ApiResponse<>("Le prénom est obligatoire", false, 400, null);
        }

        // Vérification nom
        if (request.getNom() == null || request.getNom().trim().isEmpty()) {
            return new ApiResponse<>("Le nom est obligatoire", false, 400, null);
        }

        // AJOUT : Validation du nom d'utilisateur
        if (request.getNomUtilisateur() == null || request.getNomUtilisateur().trim().isEmpty()) {
            return new ApiResponse<>("Le nom d'utilisateur est obligatoire", false, 400, null);
        }

        // Optionnel : Vérifier qu'il n'y a pas d'espaces ou caractères spéciaux
        if (!request.getNomUtilisateur().matches("^[a-zA-Z0-9._-]+$")) {
            return new ApiResponse<>("Le nom d'utilisateur ne doit contenir que des lettres, chiffres, points ou tirets.", false, 400, null);
        }

        return new ApiResponse<>("Validation réussie", true, 200, null);
    }

    /**
     * Validation du format de téléphone (E.164)
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Validation basique d'email
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    /**
     * Vérifie si le téléphone existe déjà
     */
    public boolean phoneExists(String telephone) {
        return userRepository.findByTelephone(telephone).isPresent();
    }

    /**
     * Crée un UserDto à partir d'une RegisterRequest
     */
    private UserDto createUserDtoFromRequest(RegisterRequest request, String username) {
        UserDto userDTO = new UserDto();
        userDTO.setNomUtilisateur(username);
//        userDTO.setEmail(request.getEmail());
        userDTO.setTelephone(request.getTelephone());
        userDTO.setPrenom(request.getPrenom());
        userDTO.setNom(request.getNom());
        userDTO.setRoleName(request.getRoleName());
        return userDTO;
    }

    /**
     * Méthode pour récupérer un utilisateur par téléphone
     */
    public ApiResponse<UserDto> getUserByPhone(String telephone) {
        try {
            Optional<User> user = userRepository.findByTelephone(telephone);
            if (user.isPresent()) {
                UserDto userDto = modelMapper.map(user.get(), UserDto.class);
                return new ApiResponse<>("Utilisateur trouvé", true, 200, userDto);
            } else {
                return new ApiResponse<>("Aucun utilisateur avec ce numéro de téléphone", false, 404, null);
            }
        } catch (Exception e) {
            return new ApiResponse<>("Erreur lors de la recherche: " + e.getMessage(), false, 500, null);
        }
    }

    /**
     * Vérifie si un téléphone est valide et disponible
     */
    public ApiResponse checkPhoneAvailability(String telephone) {
        if (!isValidPhoneNumber(telephone)) {
            return new ApiResponse<>("Format de téléphone invalide", false, 400, null);
        }

        if (phoneExists(telephone)) {
            return new ApiResponse<>("Numéro de téléphone déjà utilisé", false, 409, null);
        }

        return new ApiResponse<>("Numéro de téléphone disponible", true, 200, null);
    }

    // Dans UserService.java, changez le retour de saveUserInDatabase
    public ApiResponse<User> saveUserInDatabase(UserDto userDTO, String keycloakUserId) { // ⬅️ CHANGEMENT
        log.info("➡️ Sauvegarde utilisateur dans la DB locale ID Keycloak={}", keycloakUserId);

        try {
            User user = modelMapper.map(userDTO, User.class);
            user.setKeycloakId(keycloakUserId);
            // Assurez-vous d'avoir une colonne 'password' dans l'entité User si vous le stockez
            // et qu'il est hashé si l'application Keycloak n'est pas la seule source d'auth.
            User savedUser = userRepository.save(user); // ⬅️ CAPTUREZ L'UTILISATEUR SAUVEGARDÉ

            log.info("   ✅ Sauvegarde réussie dans la DB locale");
            // Retournez l'objet User pour le CompteService
            return new ApiResponse<>("Utilisateur enregistré avec succès.", true, 200, savedUser); // ⬅️ CHANGEMENT

        } catch (Exception e) {
            log.error("❌ Erreur DB : {}", e.getMessage(), e);
            return new ApiResponse<>("Erreur DB : " + e.getMessage(), false, 500, null);
        }
    }
    // Méthode addUser existante (inchangée)
    @Transactional
    public ApiResponse addUser(UserDto userDTO) {
        try {
            // Vérification si le nom d'utilisateur existe déjà
            ApiResponse<String> usernameCheck = keycloakService.usernameExists(userDTO.getNomUtilisateur());
            if (!usernameCheck.isSuccess()) {
                return new ApiResponse<>("Nom d'utilisateur déjà pris : " + userDTO.getNomUtilisateur(), false, 409, null);
            }

            // Vérification si le téléphone existe déjà
            if (phoneExists(userDTO.getTelephone())) {
                return new ApiResponse<>("Numéro de téléphone déjà enregistré : " + userDTO.getTelephone(), false, 409, null);
            }

            // Vérification si le rôle existe dans Keycloak
            Boolean roleCheck = keycloakService.roleExists(userDTO.getRoleName());
            if (!roleCheck) {
                return new ApiResponse<>("Le rôle spécifié n'existe pas dans Keycloak : " + userDTO.getRoleName(), false, 400, null);
            }

            // Création de l'utilisateur dans Keycloak
            try {
                String keycloakUserId = keycloakService.createUser(userDTO);
                ApiResponse response = keycloakService.addRoleToUser(keycloakUserId, userDTO.getRoleName());

                if (response.isSuccess() && keycloakUserId != null) {
                    ApiResponse saveResponse = saveUserInDatabase(userDTO, keycloakUserId);
                    if (saveResponse.isSuccess()) {
                        return new ApiResponse<>("Utilisateur créé avec succès dans Keycloak et dans la base de données locale", true, 201, null);
                    } else {
                        return saveResponse;
                    }
                } else {
                    return new ApiResponse<>("Erreur lors de l'ajout du rôle à l'utilisateur dans Keycloak", false, 500, null);
                }

            } catch (Exception e) {
                return new ApiResponse<>("Erreur lors de la création de l'utilisateur dans Keycloak" + e.getMessage(), false, 500, null);
            }

        } catch (Exception e) {
            return new ApiResponse<>("Erreur lors de la création de l'utilisateur : " + e.getMessage(), false, 500, null);
        }
    }

    // Les autres méthodes existantes (updateUser, getAllUsers) restent inchangées...
    @Transactional
    public ApiResponse updateUser(String userId, UserDto userDTO) {
        try {
            Optional<User> optionalUser = userRepository.findByKeycloakId(userId);
            if (!optionalUser.isPresent()) {
                return new ApiResponse<>("Utilisateur non trouvé avec l'ID : " + userId, false, 404, null);
            }

            User existingUser = optionalUser.get();

            if (!existingUser.getNomUtilisateur().equals(userDTO.getNomUtilisateur())) {
                ApiResponse<String> usernameCheck = keycloakService.usernameExists(userDTO.getNomUtilisateur());
                if (!usernameCheck.isSuccess()) {
                    return new ApiResponse<>("Nom d'utilisateur déjà pris : " + userDTO.getNomUtilisateur(), false, 409, null);
                }
            }

            // Vérification du téléphone
            if (!existingUser.getTelephone().equals(userDTO.getTelephone())) {
                if (phoneExists(userDTO.getTelephone())) {
                    return new ApiResponse<>("Numéro de téléphone déjà enregistré : " + userDTO.getTelephone(), false, 409, null);
                }
            }

            ApiResponse keycloakUpdateResponse = keycloakService.updateUser(existingUser.getKeycloakId(), userDTO);

            if (keycloakUpdateResponse.isSuccess()) {
                String savedKeycloakId = existingUser.getKeycloakId();
                Integer savedUserId = existingUser.getId();
                modelMapper.map(userDTO, existingUser);
                existingUser.setKeycloakId(savedKeycloakId);
                existingUser.setId(savedUserId);
                userRepository.save(existingUser);

                return new ApiResponse<>("Utilisateur mis à jour avec succès", true, 200, null);
            } else {
                return new ApiResponse<>("Erreur lors de la mise à jour dans Keycloak", false, 500, null);
            }
        } catch (Exception e) {
            return new ApiResponse<>("Erreur lors de la mise à jour de l'utilisateur : " + e.getMessage(), false, 500, null);
        }
    }

    // Liste de tous les utilisateurs
    public ApiResponse<List<UserDto>> getAllUsers() {
        try {
            List<UserDto> users = userRepository.findAll()
                    .stream()
                    .map(user -> modelMapper.map(user, UserDto.class))
                    .collect(Collectors.toList());
            return new ApiResponse<>("Utilisateurs récupérés avec succès", true, 200, users);
        } catch (Exception e) {
            return new ApiResponse<>("Erreur lors de la récupération des utilisateurs: " + e.getMessage(), false, 500, null);
        }
    }
}












