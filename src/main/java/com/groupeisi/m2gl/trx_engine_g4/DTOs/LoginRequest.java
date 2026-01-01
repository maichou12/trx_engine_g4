package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginRequest {

    @JsonProperty("nom_utilisateur")
    private String nom_utilisateur;

    private String password;
}
