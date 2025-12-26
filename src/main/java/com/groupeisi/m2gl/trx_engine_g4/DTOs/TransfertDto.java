package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor  // ✅ AJOUTÉ (requis par Swagger)
@AllArgsConstructor  // ✅ AJOUTÉ
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ AJOUTÉ
@Schema(description = "DTO pour effectuer un transfert")  // ✅ AJOUTÉ
public class TransfertDto {

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit etre superieur a 0")  // ✅ Utilisez @Positive au lieu de @Min
    @Schema(description = "Montant du transfert en CFA", example = "5000.0", required = true)
    private Float montant;

    @NotNull(message = "Le compte emetteur est obligatoire")
    @Schema(description = "UUID du compte emetteur", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID compteEmetteur;

    @NotNull(message = "Le compte recepteur est obligatoire")
    @Schema(description = "UUID du compte recepteur", example = "987e6543-e21b-98d7-b654-123456789abc", required = true)
    private UUID compteRecepteur;
}