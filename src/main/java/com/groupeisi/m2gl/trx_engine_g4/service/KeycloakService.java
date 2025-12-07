package com.groupeisi.m2gl.trx_engine_g4.service;

import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.UserDto;

@Service
public class KeycloakService {
    private final Keycloak keycloak;
    private final String realm;

    @Autowired
    public KeycloakService(
            @Value("${keycloak.auth-server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm, // C'est trx_engine_g4
            @Value("${keycloak.admin.username}") String adminUsername,
            @Value("${keycloak.admin.password}") String adminPassword) {

        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // <--- CORRECTION ICI : L'admin se connecte via le realm 'master'
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .grantType(OAuth2Constants.PASSWORD)
                .build();

        // On garde votre realm cible pour les opérations (create/search users)
        this.realm = realm;
    }

    public String createUser(UserDto userDTO) {
        UserRepresentation userRepresentation = getUserRepresentation(userDTO);
        Response response = keycloak.realm(realm).users().create(userRepresentation);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
        }
        URI location = response.getLocation();
        return location.getPath().replaceAll(".*/([^/]+)$", "$1");
    }

    public ApiResponse updateUser(String userId, UserDto userDTO) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            if (userResource == null) {
                return new ApiResponse("Utilisateur non trouvé dans Keycloak", false, 404, null);
            }
            UserRepresentation userRepresentation = getUserRepresentation(userDTO);
            userResource.update(userRepresentation);
            return new ApiResponse("Utilisateur mis à jour avec succès dans Keycloak", true, 200, null);

        } catch (Exception e) {
            return new ApiResponse("Erreur lors de la mise à jour de l'utilisateur dans Keycloak : " + e.getMessage(),false, 500, null);
        }
    }

    public ApiResponse addRoleToUser(String keycloakUserId, String roleName) {
        try {
            RoleRepresentation role = null;
            try {
                role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            } catch (Exception e) {
                return new ApiResponse<>("Le rôle '" + roleName + "' n'existe pas.", false, 404, null);
            }

            keycloak.realm(realm).users().get(keycloakUserId).roles().realmLevel().add(Collections.singletonList(role));
            return new ApiResponse<>("Rôle ajouté avec succès à l'utilisateur.", true, 200, null);
        } catch (Exception e) {
            return new ApiResponse<>("Erreur lors de l'ajout du rôle à l'utilisateur : " + e.getMessage(), false, 500, null);
        }
    }

    private UserRepresentation getUserRepresentation(UserDto userDTO) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername(userDTO.getNomUtilisateur());
        // userRepresentation.setEmail(userDTO.getEmail());
        userRepresentation.setFirstName(userDTO.getPrenom());
        userRepresentation.setLastName(userDTO.getNom());
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");
        credential.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credential));
        return userRepresentation;
    }

    public ApiResponse<String> usernameExists(String username) {
        UsersResource usersResource = keycloak.realm(realm).users();
        try {
            List<UserRepresentation> usersByUsername = usersResource.search(username, true);
            if (!usersByUsername.isEmpty()) {
                return new ApiResponse<>("Username '" + username + "' already exists.", false, 409, null);
            }
            return new ApiResponse<>("Username '" + username + "' is available.", true, 200, null);
        } catch (Exception e) {
            return new ApiResponse<>("An error occurred while checking the username.", false, 500, e.getMessage());
        }
    }

    public ApiResponse<String> emailExists(String email) {
        UsersResource usersResource = keycloak.realm(realm).users();

        try {
            List<UserRepresentation> usersByEmail = usersResource.search(email, null, null, null);
            if (!usersByEmail.isEmpty()) {
                return new ApiResponse<>("Email '" + email + "' is already registered.", false, 409, null);
            }
            return new ApiResponse<>("Email '" + email + "' is available.", true, 200, null);
        } catch (Exception e) {
            return new ApiResponse<>("An error occurred while checking the email.", false, 500, e.getMessage());
        }
    }

    // Méthode pour vérifier si un rôle existe dans Keycloak
    public Boolean roleExists(String roleName) {
        try {
            List<RoleRepresentation> roles = keycloak.realm(realm).roles().list();
            boolean roleExists = roles.stream().anyMatch(role -> role.getName().equals(roleName));

            if (roleExists) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public ApiResponse deleteUser(String keycloakUserId) {
        try {
            // 1. Obtenir la ressource UsersResource du Realm
            UsersResource usersResource = keycloak.realm(realm).users();

            // 2. Supprimer l'utilisateur via son ID
            Response response = usersResource.delete(keycloakUserId);

            // 3. Vérifier la réponse
            if (response.getStatus() == 204) { // HTTP 204 No Content indique un succès
                return new ApiResponse<>("Utilisateur Keycloak supprimé avec succès.", true, 204, null);
            } else if (response.getStatus() == 404) {
                return new ApiResponse<>("Utilisateur Keycloak non trouvé pour la suppression.", false, 404, null);
            } else {
                // Gérer les autres codes d'erreur
                return new ApiResponse<>("Échec de la suppression Keycloak. Code: " + response.getStatus(), false, response.getStatus(), null);
            }

        } catch (Exception e) {
            // Gérer les exceptions potentielles (e.g., problème de connexion Keycloak)
            return new ApiResponse<>("Erreur inattendue lors de la suppression Keycloak : " + e.getMessage(), false, 500, null);
        }
    }
}
