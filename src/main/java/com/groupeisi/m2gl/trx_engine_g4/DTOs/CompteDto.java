package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompteDto {
    private float solde;

    @NotNull(message = "Le numéro de compte est obligatoire")
    private UUID numCompte;

    private Integer codeMarchant;

    @NotBlank(message = "Le type de compte est obligatoire")
    private String typeCompte;
}