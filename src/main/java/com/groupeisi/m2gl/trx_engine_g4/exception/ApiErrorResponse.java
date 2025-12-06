package com.groupeisi.m2gl.trx_engine_g4.exception;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private List<String> messages;

    // Constructor, getters, and setters
    public ApiErrorResponse(int status, String error, List<String> messages) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.messages = messages;
    }

}
