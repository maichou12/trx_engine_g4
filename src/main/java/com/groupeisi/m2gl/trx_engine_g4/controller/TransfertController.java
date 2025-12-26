package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.TransfertDto;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.service.TransfertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transferts")
@RequiredArgsConstructor
@Tag(name = "Transferts", description = "API de gestion des transferts d'argent entre comptes")
public class TransfertController {

    private final TransfertService transfertService;

    @PostMapping
    @Operation(
            summary = "Effectuer un transfert d'argent entre deux comptes",
            description = "Effectue un transfert d'argent d'un compte émetteur vers un compte récepteur. " +
                    "Vérifie que : le compte émetteur a un solde suffisant, les comptes sont actifs (ENABLE), " +
                    "les comptes sont différents, et le compte émetteur n'est pas bloqué. " +
                    "Le solde des deux comptes est mis à jour automatiquement."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transfert effectué avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Erreur métier (solde insuffisant, comptes identiques, compte bloqué)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Compte émetteur ou récepteur non trouvé")
    })
    public ApiResponse createTransfert(@RequestBody @Valid TransfertDto transfertDto) {
        return transfertService.effectuerTransfert(transfertDto);
    }

    @GetMapping("/user/{phoneNumber}")
    @Operation(
            summary = "Récupérer l'historique des transferts d'un utilisateur",
            description = "Retourne la liste de tous les transferts (émissions et réceptions) effectués par un utilisateur, " +
                    "identifié par son numéro de téléphone. Inclut les détails de chaque transaction (montant, date, compte émetteur/récepteur)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste des transferts récupérée avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aucun utilisateur trouvé avec ce numéro de téléphone")
    })
    public ApiResponse getTransfertsByUser(
            @Parameter(description = "Numéro de téléphone au format international", 
                       example = "+221771234567", required = true)
            @PathVariable String phoneNumber) {
        return transfertService.getTransfertsByUser(phoneNumber);
    }
}
