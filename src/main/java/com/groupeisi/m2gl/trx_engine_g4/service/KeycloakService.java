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
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.admin.username}") String adminUsername,
            @Value("${keycloak.admin.password}") String adminPassword) {

        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .grantType(OAuth2Constants.PASSWORD)
                .build();

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
                return new ApiResponse("Utilisateur non trouve dans Keycloak", 404, false); 
            }
            UserRepresentation userRepresentation = getUserRepresentation(userDTO);
            userResource.update(userRepresentation);
            return new ApiResponse("Utilisateur mis a jour avec succes dans Keycloak", 200, null); 

        } catch (Exception e) {
            return new ApiResponse("Erreur lors de la mise a jour de l'utilisateur dans Keycloak : " + e.getMessage(), 500, false); 
        }
    }

    public ApiResponse addRoleToUser(String keycloakUserId, String roleName) {
        try {
            RoleRepresentation role = null;
            try {
                role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            } catch (Exception e) {
                return new ApiResponse("Le role '" + roleName + "' n'existe pas.", 404, false); 
            }

            keycloak.realm(realm).users().get(keycloakUserId).roles().realmLevel().add(Collections.singletonList(role));
            return new ApiResponse("Role ajoute avec succes a l'utilisateur.", 200, null); 
        } catch (Exception e) {
            return new ApiResponse("Erreur lors de l'ajout du role a l'utilisateur : " + e.getMessage(), 500, false); 
        }
    }

    private UserRepresentation getUserRepresentation(UserDto userDTO) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername(userDTO.getNomUtilisateur());
        userRepresentation.setFirstName(userDTO.getPrenom());
        userRepresentation.setLastName(userDTO.getNom());
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(userDTO.getPassword() != null ? userDTO.getPassword() : "password");
        credential.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credential));
        return userRepresentation;
    }

    public ApiResponse usernameExists(String username) {
        UsersResource usersResource = keycloak.realm(realm).users();
        try {
            List<UserRepresentation> usersByUsername = usersResource.search(username, true);
            if (!usersByUsername.isEmpty()) {
                return new ApiResponse("Username '" + username + "' already exists.", 409, false); 
            }
            return new ApiResponse("Username '" + username + "' is available.", 200, null); 
        } catch (Exception e) {
            return new ApiResponse("An error occurred while checking the username.", 500, e.getMessage()); 
        }
    }

    public ApiResponse emailExists(String email) {
        UsersResource usersResource = keycloak.realm(realm).users();

        try {
            List<UserRepresentation> usersByEmail = usersResource.search(email, null, null, null);
            if (!usersByEmail.isEmpty()) {
                return new ApiResponse("Email '" + email + "' is already registered.", 409, false); 
            }
            return new ApiResponse("Email '" + email + "' is available.", 200, null); 
        } catch (Exception e) {
            return new ApiResponse("An error occurred while checking the email.", 500, e.getMessage()); 
        }
    }

    public Boolean roleExists(String roleName) {
        try {
            List<RoleRepresentation> roles = keycloak.realm(realm).roles().list();
            boolean roleExists = roles.stream().anyMatch(role -> role.getName().equals(roleName));
            return roleExists; // ✅ Simplifié
        } catch (Exception e) {
            return false;
        }
    }

    public ApiResponse deleteUser(String keycloakUserId) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            Response response = usersResource.delete(keycloakUserId);

            if (response.getStatus() == 204) {
                return new ApiResponse("Utilisateur Keycloak supprime avec succes.", 204, null); 
            } else if (response.getStatus() == 404) {
                return new ApiResponse("Utilisateur Keycloak non trouve pour la suppression.", 404, false); 
            } else {
                return new ApiResponse("Echec de la suppression Keycloak. Code: " + response.getStatus(), response.getStatus(), false); 
            }

        } catch (Exception e) {
            return new ApiResponse("Erreur inattendue lors de la suppression Keycloak : " + e.getMessage(), 500, false); 
        }
    }
}