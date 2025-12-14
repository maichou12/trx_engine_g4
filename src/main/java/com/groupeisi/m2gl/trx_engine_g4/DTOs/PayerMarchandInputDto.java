package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import lombok.Data;
import java.util.UUID;

@Data
public class PayerMarchandInputDto {
    private float montant;
    private UUID compteEmetteur;
    private UUID compteRecepteur;


}
