package com.groupeisi.m2gl.trx_engine_g4.mutation;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.PayerMarchandInputDto;
import com.groupeisi.m2gl.trx_engine_g4.entities.Transfert;
import com.groupeisi.m2gl.trx_engine_g4.exception.ApiResponse;
import com.groupeisi.m2gl.trx_engine_g4.service.TransfertService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TransfertMutationResolver {

    private final TransfertService transfertService;

    @MutationMapping
    public ApiResponse<Transfert> payerMarchand(@Argument PayerMarchandInputDto input) {
        return transfertService.payerMarchand(
                input.getCompteEmetteur(),
                input.getCompteRecepteur(),
                input.getMontant()
        );
    }
}
