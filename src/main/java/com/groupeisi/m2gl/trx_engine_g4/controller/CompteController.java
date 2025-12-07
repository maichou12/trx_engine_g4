package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.request.OtpValidationRequest;
import com.groupeisi.m2gl.trx_engine_g4.service.CompteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compte")
public class CompteController {

    private final CompteService compteService;

    @Autowired
    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<ApiResponse> validateOtp(@Valid @RequestBody OtpValidationRequest request) {

        ApiResponse response = compteService.validateOtpAndEnableCompte(
                request.getTelephone(),
                request.getOtpCode()
        );

        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
        }
    }
}
