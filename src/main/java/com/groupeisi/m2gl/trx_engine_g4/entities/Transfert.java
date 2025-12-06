package com.groupeisi.m2gl.trx_engine_g4.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transfert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    private float montant;


    private LocalDate dateTransfert;


    @OneToOne(cascade = CascadeType.ALL)
    private DetailsTransaction detailsTransaction;
}
