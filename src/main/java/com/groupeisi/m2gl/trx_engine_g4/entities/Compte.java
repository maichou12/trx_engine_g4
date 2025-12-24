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

    private String typeCompte; // "CLIENT" ou "MARCHANT"

    private LocalDate dateCreation;

    private String status;
    private String otpCode;
    private Long otpExpiryTime;

    // 🔥 MODIFICATION : Accepter les deux orthographes
    public boolean isMarchand() {
        return "MARCHAND".equals(this.typeCompte) || "MARCHANT".equals(this.typeCompte);
    }
    
    public void debiter(float montant) {
        if (montant <= 0) throw new IllegalArgumentException("Montant invalide: " + montant);
        if (this.solde < montant) {
            throw new IllegalStateException(
                String.format("Solde insuffisant: %.2f F, tentative: %.2f F", this.solde, montant)
            );
        }
        this.solde -= montant;
    }
    
    public void crediter(float montant) {
        if (montant <= 0) throw new IllegalArgumentException("Montant invalide: " + montant);
        this.solde += montant;
    }
    
    public boolean isActif() {
        return "ACTIVE".equalsIgnoreCase(this.status) || "ENABLE".equalsIgnoreCase(this.status);
    }
    
    public boolean getMarchand() {
        return isMarchand();
    }
}