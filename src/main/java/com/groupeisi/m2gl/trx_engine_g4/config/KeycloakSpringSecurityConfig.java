package com.groupeisi.m2gl.trx_engine_g4.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class KeycloakSpringSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {}) // Active la configuration CORS par défaut
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS - PAS BESOIN D'AUTHENTIFICATION
                        .requestMatchers("/api/users/register/client").permitAll()
                        .requestMatchers("/api/users/register/marchant").permitAll()
                        .requestMatchers("/api/users/getUserByPhone/{phone}").permitAll()
                        .requestMatchers("/api/compte/validate-otp").permitAll()
                        .requestMatchers("/api/users/validate-otp").permitAll()
                        
                        // 🔥 AJOUT: ENDPOINTS DE PAIEMENT POUR TESTS
                        .requestMatchers("/api/payments/**").permitAll()
                        
                        // ENDPOINTS PUBLIC
                        .requestMatchers("/public/**", "/").permitAll()
                        
                        // ENDPOINTS HEALTH CHECK
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/health").permitAll()
                        
                        // PROTECTED ENDPOINTS - BESOIN D'AUTHENTIFICATION
                        .requestMatchers("/api/transferts").authenticated()
                        .anyRequest().authenticated()
                )
                // Configuration Resource Server (JWT) pour les routes protégées
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    // Convertisseur de rôles Keycloak (votre code original conservé tel quel)
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get("trx-engine-g4-client");
                if (clientRoles != null) {
                    List<String> roles = (List<String>) clientRoles.get("roles");
                    if (roles != null) {
                        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    }
                }
            }
            return authorities;
        });
        return converter;
    }
}