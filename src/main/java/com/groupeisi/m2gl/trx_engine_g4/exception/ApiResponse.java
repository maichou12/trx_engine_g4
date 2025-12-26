package com.groupeisi.m2gl.trx_engine_g4.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Reponse standard de l'API")
public class ApiResponse {

    @Schema(description = "Message de reponse", example = "Operation reussie")
    private String message;

    @Schema(description = "Indicateur de succes", example = "true")
    private boolean success;

    @Schema(description = "Code de statut HTTP", example = "200")
    private int statusCode;

    @Schema(description = "Donnees de reponse")
    private Object data;

    // Constructeur de Succes
    public ApiResponse(String message, int statusCode, Object data) {
        this.message = message;
        this.success = true;
        this.statusCode = statusCode;
        this.data = data;
    }

    // Constructeur d'Erreur
    public ApiResponse(String message, int statusCode, boolean success) {
        this.message = message;
        this.success = success;
        this.statusCode = statusCode;
        this.data = null;
    }
}