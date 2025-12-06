package com.groupeisi.m2gl.trx_engine_g4.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String prenom;
    private String nom;
    private String nomUtilisateur;
    private String telephone;
    private String password;
    private String roleName;
    private Long nin;
}
