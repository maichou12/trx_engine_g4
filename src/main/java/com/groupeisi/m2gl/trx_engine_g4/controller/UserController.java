package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.CompleteProfileRequest;
import com.groupeisi.m2gl.trx_engine_g4.DTOs.UserDto;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.request.RegisterRequest;
import com.groupeisi.m2gl.trx_engine_g4.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs", description = "API de gestion des utilisateurs (inscription, profil, consultation)")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/add")
    @Operation(
            summary = "Ajouter un utilisateur manuellement (admin)",
            description = "Crée un utilisateur dans Keycloak et dans la base de données locale. " +
                    "Cette méthode est généralement utilisée par les administrateurs pour créer des utilisateurs manuellement."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Nom d'utilisateur ou email déjà existant"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Erreur lors de la création dans Keycloak")
    })
    public ApiResponse addUser(@Valid @RequestBody UserDto userDto) {
        return userService.addUser(userDto);
    }

    @PutMapping("/update/{userId}")
    @Operation(
            summary = "Mettre à jour les informations d'un utilisateur",
            description = "Met à jour les informations d'un utilisateur existant (nom, prénom, téléphone, etc.) " +
                    "dans Keycloak et dans la base de données locale. L'userId est l'identifiant Keycloak de l'utilisateur."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Utilisateur mis à jour avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ApiResponse updateUser(
            @Parameter(description = "Identifiant Keycloak de l'utilisateur", required = true)
            @PathVariable String userId, 
            @Valid @RequestBody UserDto userDto) {
        return userService.updateUser(userId, userDto);
    }

    @PostMapping("/register/client")
    @Operation(
            summary = "Inscription d'un nouveau client",
            description = "Inscrit un nouveau client dans le système. Cette opération : " +
                    "1. Crée l'utilisateur dans Keycloak avec le rôle 'user', " +
                    "2. Sauvegarde l'utilisateur dans la base de données locale, " +
                    "3. Crée un compte client avec statut DISABLE, " +
                    "4. Envoie un code OTP par SMS pour activer le compte. " +
                    "Le compte doit être activé via /api/compte/validate-otp avant utilisation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Client inscrit avec succès, OTP envoyé par SMS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Numéro de téléphone ou nom d'utilisateur déjà utilisé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données de validation invalides")
    })
    public ResponseEntity<ApiResponse> registerClient(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(registerRequest));
    }

    @PostMapping("/register/marchant")
    @Operation(
            summary = "Inscription d'un nouveau marchand",
            description = "Inscrit un nouveau marchand dans le système. Cette opération : " +
                    "1. Crée l'utilisateur dans Keycloak avec le rôle spécifié (ou 'user' par défaut), " +
                    "2. Sauvegarde l'utilisateur dans la base de données locale. " +
                    "Note: Le compte marchand doit être créé séparément via /api/compte/create-marchand/{telephone}."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Marchand inscrit avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Numéro de téléphone ou nom d'utilisateur déjà utilisé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données de validation invalides")
    })
    public ResponseEntity<ApiResponse> registerMarchant(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUserMarchant(registerRequest));
    }

    @GetMapping("/getUserByPhone/{phone}")
    @Operation(
            summary = "Récupérer un utilisateur par son numéro de téléphone",
            description = "Retourne les informations complètes d'un utilisateur (nom, prénom, rôle, compte associé, etc.) " +
                    "en utilisant son numéro de téléphone au format international."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aucun utilisateur trouvé avec ce numéro de téléphone")
    })
    public ResponseEntity<ApiResponse> getUserByPhone(
            @Parameter(description = "Numéro de téléphone au format international (ex: +221771234567)", 
                       example = "+221771234567", required = true)
            @PathVariable String phone) {
        return ResponseEntity.ok(userService.getUserByPhone(phone));
    }

    @PostMapping("/complete-profile")
    @Operation(
            summary = "Compléter le profil d'un utilisateur",
            description = "Permet de compléter ou mettre à jour les informations du profil utilisateur " +
                    "après l'inscription initiale (ex: ajout du NIN, mise à jour des informations personnelles)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profil complété avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Utilisateur non trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<ApiResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        return ResponseEntity.ok(userService.completeProfile(request));
    }
}
