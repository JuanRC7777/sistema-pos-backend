package com.pos.domain.exception;

public class TurnoYaAbiertoException extends RuntimeException {
    public TurnoYaAbiertoException() {
        super("Ya existe un turno abierto. Debe cerrar el turno actual antes de abrir uno nuevo.");
    }
}
