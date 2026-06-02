package com.pos.application.port.in.venta;

import com.pos.application.dto.command.RegistrarVentaCommand;
import com.pos.application.dto.response.VentaResponse;

public interface RegistrarVentaUseCase {
    VentaResponse registrar(RegistrarVentaCommand cmd);
}
