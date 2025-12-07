package com.groupeisi.m2gl.trx_engine_g4.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transfert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    private float montant;


    private LocalDateTime dateTransfert;


    @OneToOne(cascade = CascadeType.ALL)
    private DetailsTransaction detailsTransaction;
}
