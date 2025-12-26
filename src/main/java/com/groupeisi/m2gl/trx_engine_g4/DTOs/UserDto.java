package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ AJOUTÉ
@Schema(description = "DTO pour les utilisateurs")  // ✅ AJOUTÉ
public class UserDto {

    @Schema(description = "Identifiant unique de l'utilisateur", example = "1")
    private Integer id;

    @Schema(description = "Identifiant Keycloak de l'utilisateur")
    private String keycloakId;

    @Schema(description = "Nom de famille", example = "Diop", required = true)
    private String nom;  // ✅ GARDE les validations dans le DTO

    @Schema(description = "Prenom", example = "Amadou", required = true)
    private String prenom;  // ✅ GARDE les validations dans le DTO

    @Schema(description = "Nom d'utilisateur unique", example = "adiop", required = true)
    private String nomUtilisateur;  // ✅ GARDE les validations dans le DTO

    @Schema(description = "Numero de telephone au format international", example = "+221771234567", required = true)
    private String telephone;  // ✅ GARDE les validations dans le DTO

    @Schema(description = "Numero d'identification nationale", example = "1234567890123")
    private Long nin;

    @Schema(description = "Role de l'utilisateur", example = "user", required = true)
    private String roleName;  // ✅ GARDE les validations dans le DTO

    @Schema(description = "Identifiant du compte associe")
    private Integer compteId;

    @Schema(description = "Mot de passe (utilise uniquement pour la creation dans Keycloak)")
    private String password;
}