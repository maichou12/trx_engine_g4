package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ActiverMarchantRequest {
    
    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(
        regexp = "^\\+221[0-9]{9}$", 
        message = "Format téléphone invalide. Utilisez: +221XXXXXXXXX"
    )
    private String telephone;
    
    @Min(value = 100000, message = "Le code marchant doit avoir 6 chiffres (min: 100000)")
    @Max(value = 999999, message = "Le code marchant doit avoir 6 chiffres (max: 999999)")
    private Integer codeMarchant;
    
    // 🔥 CONSTRUCTEUR POUR TESTS
    public ActiverMarchantRequest() {}
    
    public ActiverMarchantRequest(String telephone, Integer codeMarchant) {
        this.telephone = telephone;
        this.codeMarchant = codeMarchant;
    }
}