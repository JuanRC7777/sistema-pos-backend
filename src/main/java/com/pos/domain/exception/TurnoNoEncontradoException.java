package com.pos.domain.exception;

public class TurnoNoEncontradoException extends RuntimeException {
    public TurnoNoEncontradoException() {
        super("No hay un turno activo en este momento.");
    }
}
