package com.pos.application.port.out;

import java.time.LocalDate;

public interface SecuenciaFacturaRepositoryPort {
    int obtenerYIncrementarSecuencia(LocalDate fecha);
}
