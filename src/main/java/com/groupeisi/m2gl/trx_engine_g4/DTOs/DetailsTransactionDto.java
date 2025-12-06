package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DetailsTransactionDto {
    @NotNull(message = "Le compte émetteur est obligatoire")
    private UUID compteEmetteur;


    @NotNull(message = "Le compte récepteur est obligatoire")
    private UUID compteRecepteur;
}
