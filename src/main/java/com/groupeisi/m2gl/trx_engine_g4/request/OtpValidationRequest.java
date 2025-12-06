package com.groupeisi.m2gl.trx_engine_g4.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpValidationRequest {

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Format de téléphone international invalide (Ex: +22177... ou +336...)")
    private String telephone;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit être composé de 6 chiffres")
    private String otpCode;
}