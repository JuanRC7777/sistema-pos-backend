package com.pos.domain.exception;

public class VentaNoEncontradaException extends RuntimeException {
    public VentaNoEncontradaException(String identificador) {
        super("Venta no encontrada: " + identificador);
    }
}
