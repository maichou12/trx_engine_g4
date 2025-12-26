package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.request.OtpValidationRequest;
import com.groupeisi.m2gl.trx_engine_g4.service.CompteService;
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
@RequestMapping("/api/compte")
@Tag(name = "Comptes", description = "API de gestion des comptes (création, validation OTP, consultation)")
public class CompteController {

    private final CompteService compteService;

    @Autowired
    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @PostMapping("/validate-otp")
    @Operation(
            summary = "Valider le code OTP et activer le compte",
            description = "Valide le code OTP reçu par SMS et active le compte (client ou marchand). " +
                    "Le compte passe du statut DISABLE à ENABLE et reçoit un solde initial de 500 000 FCFA. " +
                    "Le code OTP expire après 5 minutes."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Compte activé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Code OTP invalide ou expiré"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Utilisateur ou compte non trouvé")
    })
    public ResponseEntity<ApiResponse> validateOtp(@Valid @RequestBody OtpValidationRequest request) {

        ApiResponse response = compteService.validateOtpAndEnableCompte(
                request.getTelephone(),
                request.getOtpCode()
        );

        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
        }
    }

    @PostMapping("/create-marchand/{telephone}")
    @Operation(
            summary = "Créer un compte marchand et envoyer le code OTP",
            description = "Crée un compte marchand pour un utilisateur existant (identifié par son numéro de téléphone) " +
                    "avec un statut DISABLE. Un code OTP à 6 chiffres est généré et envoyé par SMS. " +
                    "Le compte doit être activé via l'endpoint /validate-otp avant d'être utilisable."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Compte marchand créé, OTP envoyé par SMS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Utilisateur non trouvé avec ce numéro de téléphone")
    })
    public ResponseEntity<ApiResponse> createMarchandCompte(
            @Parameter(description = "Numéro de téléphone au format international (ex: +221771234567)", 
                       example = "+221771234567", required = true)
            @PathVariable String telephone) {

        ApiResponse response =
                compteService.createMerchantCompte(telephone);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    @GetMapping("/by-phone/{telephone}")
    @Operation(
            summary = "Récupérer les informations d'un compte par numéro de téléphone",
            description = "Retourne les détails du compte associé à un utilisateur (solde, statut, type de compte, etc.) " +
                    "en utilisant le numéro de téléphone de l'utilisateur."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Compte trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aucun compte trouvé pour ce numéro de téléphone")
    })
    public ResponseEntity<ApiResponse> getCompteByPhone(
            @Parameter(description = "Numéro de téléphone au format international", 
                       example = "+221771234567", required = true)
            @PathVariable String telephone) {
        ApiResponse response = compteService.getCompteByPhone(telephone);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
