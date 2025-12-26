package com.groupeisi.m2gl.trx_engine_g4.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Vérification de l'état de l'API")
public class HealthController {

    @GetMapping
    @Operation(
            summary = "Vérifier l'état de l'API",
            description = "Endpoint de santé pour vérifier que l'API est opérationnelle. " +
                    "Retourne 'OK' si l'application fonctionne correctement."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API opérationnelle")
    })
    public String health() {
        return "OK";
    }
}
