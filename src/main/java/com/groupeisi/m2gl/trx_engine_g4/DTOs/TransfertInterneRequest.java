package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TransfertInterneRequest {
    
    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(
        regexp = "^\\+221[0-9]{9}$", 
        message = "Format téléphone invalide. Utilisez: +221XXXXXXXXX"
    )
    private String telephone;
    
    @Min(value = 100, message = "Le montant minimum est de 100 FCFA")
    private float amount;
    
    @NotBlank(message = "La direction est obligatoire")
    @Pattern(
        regexp = "^(TO_CLIENT|TO_MARCHAND)$", 
        message = "Direction invalide. Options: TO_CLIENT ou TO_MARCHAND"
    )
    private String direction;  // TO_CLIENT ou TO_MARCHAND
    
    private String motif;  // Optionnel: "Dépenses perso", "Réappro business", etc.
    
    // 🔥 CONSTRUCTEUR POUR TESTS
    public TransfertInterneRequest() {}
    
    public TransfertInterneRequest(String telephone, float amount, String direction) {
        this.telephone = telephone;
        this.amount = amount;
        this.direction = direction;
        this.motif = "Transfert interne " + direction;
    }
}