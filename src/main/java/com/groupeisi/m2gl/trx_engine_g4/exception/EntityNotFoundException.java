package com.groupeisi.m2gl.trx_engine_g4.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityNotFoundException extends RuntimeException {
    String message;
    int code;
}
