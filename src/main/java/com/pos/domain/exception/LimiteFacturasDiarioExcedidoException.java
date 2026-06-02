package com.pos.domain.exception;

public class LimiteFacturasDiarioExcedidoException extends RuntimeException {
    public LimiteFacturasDiarioExcedidoException() {
        super("Se alcanzó el límite de 999.999 facturas diarias");
    }
}
