package com.groupeisi.m2gl.trx_engine_g4.DTOs;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransfertDto {
    @NotNull(message = "Le montant est obligatoire")
    private Float montant;

    @NotNull(message = "La date du transfert est obligatoire")
    private String dateTransfert;

    @NotNull(message = "Les détails de transaction sont obligatoires")
    private DetailsTransactionDto details;
}
