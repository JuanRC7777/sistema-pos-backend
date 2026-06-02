package com.pos.application.port.in.auth;

import com.pos.application.dto.command.LoginCommand;
import com.pos.application.dto.response.LoginResponse;

public interface LoginUseCase {
    LoginResponse login(LoginCommand cmd);
}
