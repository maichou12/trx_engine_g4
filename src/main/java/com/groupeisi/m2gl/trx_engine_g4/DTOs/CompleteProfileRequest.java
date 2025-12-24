package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteProfileRequest {
    private String telephone;
    private String nom;
    private String prenom;
    private String nin;
}
