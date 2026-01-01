package com.groupeisi.m2gl.trx_engine_g4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients
public class TrxEngineG4Application {

    public static void main(String[] args) {
        SpringApplication.run(TrxEngineG4Application.class, args);
    }

}
