package com.groupeisi.m2gl.trx_engine_g4.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // ✅ AJOUTÉ
@AllArgsConstructor  // ✅ AJOUTÉ
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ AJOUTÉ
@Schema(description = "Requete d'inscription utilisateur")  // ✅ AJOUTÉ
public class RegisterRequest {

    @Schema(description = "Prenom de l'utilisateur", example = "Amadou", required = true)
    private String prenom;

    @Schema(description = "Nom de l'utilisateur", example = "Diop", required = true)
    private String nom;

    @Schema(description = "Nom d'utilisateur unique", example = "adiop", required = true)
    private String nomUtilisateur;

    @Schema(description = "Numero de telephone au format international", example = "+221771234567", required = true)
    private String telephone;

    @Schema(description = "Mot de passe", example = "Password123!", required = true)
    private String password;

    @Schema(description = "Role de l'utilisateur", example = "user")
    private String roleName;

    @Schema(description = "Numero d'identification nationale", example = "1234567890123")
    private Long nin;
}