package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // ✅ AJOUTÉ (requis par Swagger)
@AllArgsConstructor  // ✅ AJOUTÉ
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ AJOUTÉ
@Schema(description = "DTO pour les comptes")  // ✅ AJOUTÉ
public class CompteDto {

    @Schema(description = "Solde du compte en CFA", example = "50000.0")
    private float solde;

    @NotNull(message = "Le numero de compte est obligatoire")
    @Schema(description = "Numero unique du compte (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID numCompte;

    @Schema(description = "Code marchand (pour les comptes marchands uniquement)", example = "123456")
    private Integer codeMarchant;

    @NotBlank(message = "Le type de compte est obligatoire")
    @Schema(description = "Type de compte", example = "CLIENT", allowableValues = {"CLIENT", "MARCHAND"})
    private String typeCompte;

    @Schema(description = "Statut du compte", example = "ENABLE", allowableValues = {"ENABLE", "DISABLE", "BLOQUE"})
    private String status;

    @Schema(description = "Date de creation du compte")
    private String dateCreation;
}