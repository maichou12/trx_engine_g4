package com.groupeisi.m2gl.trx_engine_g4.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MarchandGraphQLClient {

    private final WebClient webClient;

    public MarchandGraphQLClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://localhost:8082/graphql")
                .build();
    }

    public Boolean verifyPassword(int userId, String password) {

        String query = """
        mutation VerifyPassword($userId: Int!, $password: String!) {
          verifyMarchandPassword(userId: $userId, password: $password)
        }
    """;

        Map<String, Object> body = Map.of(
                "query", query,
                "variables", Map.of(
                        "userId", userId,
                        "password", password
                )
        );

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {

                    // 🔴 Cas erreur GraphQL
                    if (json.has("errors")) {
                        throw new RuntimeException(
                                json.get("errors").get(0).get("message").asText()
                        );
                    }

                    // 🟢 Cas normal
                    return json.get("data")
                            .get("verifyMarchandPassword")
                            .asBoolean();
                })
                .block();
    }

}

