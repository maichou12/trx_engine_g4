package com.groupeisi.m2gl.trx_engine_g4.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {
    @Value("${keycloak.client-key-password}")
    private String keycloakClientSecret;

    // Nouveau champ pour le VRAI secret client
    @Value("${keycloak.admin-client-secret}")
    private String adminClientSecret;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Value("${keycloak.client-id}") // Ajout pour récupérer le vrai Client ID
    private String keycloakClientId; // Nouveau champ

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakAuthServerUrl)
                .realm(keycloakRealm) // <--- Utilisez votre propre Realm : trx_engine_g4
                .grantType("client_credentials") // <--- Changement de type d'accès
                .clientId(keycloakClientId) // <--- Utilisez votre Client ID : trx-engine-g4-client
                .clientSecret(keycloakClientSecret) // <--- Utilisez votre Client Secret
                // Supprimez .username et .password, ils ne sont pas utilisés pour client_credentials
                .build();
    }
}
