package com.pos.application.dto.response;

public class LoginResponse {
    private String idToken;
    private String accessToken;
    private int expiresIn;

    public LoginResponse() {}

    public LoginResponse(String idToken, String accessToken, int expiresIn) {
        this.idToken = idToken;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
}
