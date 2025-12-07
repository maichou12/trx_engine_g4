package com.groupeisi.m2gl.trx_engine_g4.DTOs;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TransfertDto {
    @NotNull(message = "Le montant est obligatoire")
    @Min(value = 1, message = "Le montant doit être supérieur à 0")
    private Float montant;

    @NotNull(message = "Le compte émetteur est obligatoire")
    private UUID compteEmetteur;

    @NotNull(message = "Le compte récepteur est obligatoire")
    private UUID compteRecepteur;
}
