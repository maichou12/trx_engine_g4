package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PaiementRequest {
    
    @NotBlank(message = "Le téléphone du client est obligatoire")
    @Pattern(
        regexp = "^\\+221[0-9]{9}$", 
        message = "Format téléphone invalide. Utilisez: +221XXXXXXXXX"
    )
    private String clientPhone;
    
    @NotBlank(message = "Le téléphone du marchand est obligatoire")
    @Pattern(
        regexp = "^\\+221[0-9]{9}$", 
        message = "Format téléphone invalide. Utilisez: +221XXXXXXXXX"
    )
    private String merchantPhone;
    
    @Min(value = 100, message = "Le montant minimum est de 100 FCFA")
    private float amount;
    
    private String motif;  // Optionnel: "Achat boutique", "Service", "Nourriture", etc.
    
    // 🔥 CONSTRUCTEUR POUR TESTS
    public PaiementRequest() {}
    
    public PaiementRequest(String clientPhone, String merchantPhone, float amount) {
        this.clientPhone = clientPhone;
        this.merchantPhone = merchantPhone;
        this.amount = amount;
        this.motif = "Paiement marchand";
    }
}