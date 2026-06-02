package com.pos.infrastructure.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.application.dto.command.LoginCommand;
import com.pos.application.dto.response.LoginResponse;
import com.pos.application.port.in.auth.LoginUseCase;
import com.pos.application.service.AuthService;
import com.pos.domain.exception.CredencialesInvalidasException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class AuthFunction implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final LoginUseCase loginUseCase;
    private final ObjectMapper objectMapper;

    // Constructor sin argumentos — Lambda lo invoca al arrancar
    public AuthFunction() {
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
            .region(Region.of(System.getenv("AWS_REGION")))
            .build();

        this.loginUseCase = new AuthService(
            cognitoClient,
            System.getenv("COGNITO_CLIENT_ID")
        );
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            LoginCommand cmd = objectMapper.readValue(event.getBody(), LoginCommand.class);
            LoginResponse response = loginUseCase.login(cmd);
            return ApiGatewayResponse.ok200(objectMapper.writeValueAsString(response));

        } catch (CredencialesInvalidasException e) {
            return ApiGatewayResponse.error(401, "Credenciales inválidas");
        } catch (Exception e) {
            context.getLogger().log("Error en AuthFunction: " + e.getMessage());
            return ApiGatewayResponse.error(500, "Error interno del servidor");
        }
    }
}
