package com.pos.application.dto.command;

public class LoginCommand {
    private String username;
    private String password;

    public LoginCommand() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
