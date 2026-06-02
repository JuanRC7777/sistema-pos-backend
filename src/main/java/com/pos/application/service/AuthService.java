package com.pos.application.service;

import com.pos.application.dto.command.LoginCommand;
import com.pos.application.dto.response.LoginResponse;
import com.pos.application.port.in.auth.LoginUseCase;
import com.pos.domain.exception.CredencialesInvalidasException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.util.Map;

public class AuthService implements LoginUseCase {

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolClientId;

    public AuthService(CognitoIdentityProviderClient cognitoClient, String userPoolClientId) {
        this.cognitoClient = cognitoClient;
        this.userPoolClientId = userPoolClientId;
    }

    @Override
    public LoginResponse login(LoginCommand cmd) {
        try {
            InitiateAuthRequest request = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(userPoolClientId)
                .authParameters(Map.of(
                    "USERNAME", cmd.getUsername(),
                    "PASSWORD", cmd.getPassword()
                ))
                .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(request);

            return new LoginResponse(
                response.authenticationResult().idToken(),
                response.authenticationResult().accessToken(),
                response.authenticationResult().expiresIn()
            );

        } catch (NotAuthorizedException e) {
            throw new CredencialesInvalidasException();
        }
    }
}
