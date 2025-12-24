package com.groupeisi.m2gl.trx_engine_g4.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String keycloakId;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String nomUtilisateur;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String telephone;

    private Long nin;

    @NotBlank(message = "Le du rôle est obligatoire")
    private String roleName;

    @OneToOne
    @JoinColumn(name = "compte_id")
    private Compte compte;

// 🔥 AJOUTER JUSTE CETTE MÉTHODE À LA FIN :
    public boolean estMarchand() {
        return this.compte != null && this.compte.isMarchand();
    }
    // creer un endPoint qui cree un compteMarchant avec code marchant et retourne un status 201 + id du compte
}
