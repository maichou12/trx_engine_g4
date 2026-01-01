package com.groupeisi.m2gl.trx_engine_g4.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-marchand", url = "http://localhost:8082")
public interface MarchandFeignClient {
   /* @PostMapping("/api/marchands/verify-password")
    Boolean verifyPassword(
            @RequestParam int userId,
            @RequestParam String password
    );*/
}
