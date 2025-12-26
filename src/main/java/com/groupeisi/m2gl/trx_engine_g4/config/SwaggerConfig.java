package com.groupeisi.m2gl.trx_engine_g4.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TRX Engine API")
                        .version("1.0")
                        .description("Documentation de l'API TRX Engine"));
    }
}