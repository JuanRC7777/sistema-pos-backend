package com.pos.application.port.out;

import com.pos.application.dto.response.TurnoResponse;
import com.pos.domain.model.Turno;

import java.math.BigDecimal;
import java.util.Optional;

public interface TurnoRepositoryPort {
    void abrir(Turno turno);
    Optional<Turno> obtenerActivo();
    TurnoResponse cerrar(Turno turno);
    void acumularVenta(String metodoPago, BigDecimal total);
}
