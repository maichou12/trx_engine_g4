package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.LoginRequest;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.service.KeycloakAuthService;
import com.groupeisi.m2gl.trx_engine_g4.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final KeycloakAuthService keycloakAuthService;
    private final UserService userService;

    public AuthController(KeycloakAuthService keycloakAuthService, UserService userService) {
        this.keycloakAuthService = keycloakAuthService;
        this.userService = userService;
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        ApiResponse response = userService.loginMarchand(request);
        System.out.println("login response: " + request.getNom_utilisateur() + " mot de passe: " + request.getPassword());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
