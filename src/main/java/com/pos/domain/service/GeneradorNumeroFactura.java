package com.pos.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GeneradorNumeroFactura {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generar(LocalDate fecha, int secuencia) {
        return String.format("FAC-%s-%06d", fecha.format(FORMATO_FECHA), secuencia);
    }

    public boolean validarFormato(String numeroFactura) {
        return numeroFactura != null && numeroFactura.matches("^FAC-\\d{8}-\\d{6}$");
    }
}
