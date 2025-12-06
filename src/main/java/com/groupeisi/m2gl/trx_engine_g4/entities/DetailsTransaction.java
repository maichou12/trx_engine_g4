package com.groupeisi.m2gl.trx_engine_g4.entities;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailsTransaction {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer id;


private UUID compteEmetteur;


private UUID compteRecepteur;
}
