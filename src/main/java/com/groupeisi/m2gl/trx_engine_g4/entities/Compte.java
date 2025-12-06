package com.groupeisi.m2gl.trx_engine_g4.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @ManyToOne
    private User user;

    private String status;
    private String otpCode;
    private Long otpExpiryTime;
}
