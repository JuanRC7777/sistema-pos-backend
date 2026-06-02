package com.pos.application.port.in.caja;

import com.pos.application.dto.command.CerrarTurnoCommand;
import com.pos.application.dto.response.TurnoResponse;

public interface CerrarTurnoUseCase {
    TurnoResponse cerrar(CerrarTurnoCommand cmd);
}
