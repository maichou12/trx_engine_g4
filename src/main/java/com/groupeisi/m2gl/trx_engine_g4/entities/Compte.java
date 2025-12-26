package com.groupeisi.m2gl.trx_engine_g4.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // ✅ AJOUTÉ
public class Compte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private float solde;

    @Column(unique = true, nullable = false)
    private UUID numCompte;

    private Integer codeMarchant;

    private String typeCompte;

    private LocalDate dateCreation;

    private String status;
    private String otpCode;
    private Long otpExpiryTime;

    // ✅ AJOUTEZ cette relation si elle existe dans votre modèle
    @OneToOne(mappedBy = "compte")
    @JsonBackReference  // Pour éviter les cycles infinis avec User
    private User user;
}