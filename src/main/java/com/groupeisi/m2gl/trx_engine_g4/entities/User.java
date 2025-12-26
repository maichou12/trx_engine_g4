package com.groupeisi.m2gl.trx_engine_g4.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_user")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // ✅ AJOUTÉ
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String keycloakId;

    // ❌ PROBLÈME : @NotBlank ne doit PAS être sur l'entité si les champs peuvent être null temporairement
    // Utilisez @NotBlank uniquement dans les DTOs ou les requests de validation
    private String nom;  // ✅ Supprimé @NotBlank et @Size

    private String prenom;  // ✅ Supprimé @NotBlank et @Size

    private String nomUtilisateur;  // ✅ Supprimé @NotBlank

    private String telephone;  // ✅ Supprimé @NotBlank

    private Long nin;

    private String roleName;  // ✅ Supprimé @NotBlank

    @OneToOne(cascade = CascadeType.ALL)  // ✅ AJOUTÉ cascade
    @JoinColumn(name = "compte_id")
    @JsonManagedReference  // ✅ AJOUTÉ pour éviter les cycles infinis
    private Compte compte;
}