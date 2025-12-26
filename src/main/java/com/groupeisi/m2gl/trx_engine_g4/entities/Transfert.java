package com.groupeisi.m2gl.trx_engine_g4.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // ✅ AJOUTÉ
public class Transfert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private float montant;

    private LocalDateTime dateTransfert;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "details_transaction_id")  // ✅ AJOUTÉ pour clarté
    private DetailsTransaction detailsTransaction;
}