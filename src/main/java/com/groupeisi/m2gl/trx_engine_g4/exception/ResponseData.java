package com.groupeisi.m2gl.trx_engine_g4.exception;
import lombok.Data;

@Data
public class ResponseData<T> {
    private boolean success;
    private T data;  // Utilisation du type générique T pour le champ data

    // Constructeur avec les paramètres success et data
    public ResponseData(boolean success, T data) {
        this.success = success;
        this.data = data;
    }
}
