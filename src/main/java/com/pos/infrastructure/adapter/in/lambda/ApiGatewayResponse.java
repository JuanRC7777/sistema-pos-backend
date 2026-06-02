package com.pos.infrastructure.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import java.util.Map;

public class ApiGatewayResponse {

    private static final Map<String, String> HEADERS = Map.of("Content-Type", "application/json");

    public static APIGatewayV2HTTPResponse ok200(String body) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(200)
            .withHeaders(HEADERS)
            .withBody(body)
            .build();
    }

    public static APIGatewayV2HTTPResponse created201(String body) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(201)
            .withHeaders(HEADERS)
            .withBody(body)
            .build();
    }

    public static APIGatewayV2HTTPResponse error(int status, String message) {
        String body = String.format(
            "{\"status\":%d,\"error\":\"%s\",\"timestamp\":\"%s\"}",
            status, message, java.time.Instant.now().toString()
        );
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(status)
            .withHeaders(HEADERS)
            .withBody(body)
            .build();
    }
}
