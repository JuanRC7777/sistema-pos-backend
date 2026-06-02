package com.pos.application.port.in.caja;

import com.pos.application.dto.command.AbrirTurnoCommand;
import com.pos.application.dto.response.TurnoResponse;

public interface AbrirTurnoUseCase {
    TurnoResponse abrir(AbrirTurnoCommand cmd);
}
