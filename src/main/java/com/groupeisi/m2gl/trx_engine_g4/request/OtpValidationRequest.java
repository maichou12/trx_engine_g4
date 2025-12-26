package com.groupeisi.m2gl.trx_engine_g4.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // ✅ AJOUTÉ
@AllArgsConstructor  // ✅ AJOUTÉ
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ AJOUTÉ
@Schema(description = "Requete de validation OTP")  // ✅ AJOUTÉ
public class OtpValidationRequest {

    @NotBlank(message = "Le numero de telephone est obligatoire")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Format de telephone international invalide (Ex: +22177... ou +336...)")
    @Schema(description = "Numero de telephone au format international", example = "+221771234567", required = true)
    private String telephone;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit etre compose de 6 chiffres")
    @Schema(description = "Code OTP a 6 chiffres", example = "123456", required = true)
    private String otpCode;
}