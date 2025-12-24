package com.groupeisi.m2gl.trx_engine_g4.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
    
    private boolean success;
    private String message;
    private String transactionId;
    private String reference;
    private float amount;
    
    private String clientPhone;
    private String merchantPhone;
    private Integer merchantCode;
    
    private float newClientBalance;
    private float newMerchantBalance;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String status;  // "COMPLETED", "PENDING", "FAILED"
    
    // 🔥 CONSTRUCTEUR POUR SUCCÈS
    public static PaymentResponseDto success(
            String transactionId, String clientPhone, String merchantPhone,
            float amount, float newClientBalance, float newMerchantBalance,
            Integer merchantCode) {
        
        PaymentResponseDto response = new PaymentResponseDto();
        response.success = true;
        response.message = "Paiement effectué avec succès";
        response.transactionId = transactionId;
        response.reference = "WAVE_" + transactionId;
        response.amount = amount;
        response.clientPhone = clientPhone;
        response.merchantPhone = merchantPhone;
        response.merchantCode = merchantCode;
        response.newClientBalance = newClientBalance;
        response.newMerchantBalance = newMerchantBalance;
        response.timestamp = LocalDateTime.now();
        response.status = "COMPLETED";
        
        return response;
    }
    
    // 🔥 CONSTRUCTEUR POUR ÉCHEC
    public static PaymentResponseDto error(String message) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.success = false;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        response.status = "FAILED";
        return response;
    }
}