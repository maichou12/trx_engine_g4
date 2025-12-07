package com.groupeisi.m2gl.trx_engine_g4.exception;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private String message;
    private boolean success;
    private int statusCode;
    private Object data;

    // Constructeurs, getters, et setters

    public ApiResponse(String message, boolean success, int statusCode, Object data) {
        this.message = message;
        this.success = success;
        this.statusCode = statusCode;
        this.data = data;
    }

    // Constructeur de Succès
    public ApiResponse(String message, int statusCode, T data) {
        this.message = message;
        this.success = true;
        this.statusCode = statusCode;
        this.data = data;
    }

    // Constructeur d'Erreur (utilisé dans le GlobalExceptionHandler)
    public ApiResponse(String message, int statusCode, boolean success) {
        this.message = message;
        this.success = success;
        this.statusCode = statusCode;
        this.data = null;
    }
}
