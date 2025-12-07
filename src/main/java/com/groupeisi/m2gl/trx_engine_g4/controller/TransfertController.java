package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.TransfertDto;
import com.groupeisi.m2gl.trx_engine_g4.entities.Transfert;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.service.TransfertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transferts")
@RequiredArgsConstructor
public class TransfertController {

    private final TransfertService transfertService;

    @PostMapping
    public ApiResponse<Transfert> createTransfert(@RequestBody @Valid TransfertDto transfertDto) {
        return transfertService.effectuerTransfert(transfertDto);
    }
}
