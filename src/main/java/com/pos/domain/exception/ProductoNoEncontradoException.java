package com.pos.domain.exception;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException(String codigo) {
        super("Producto no encontrado con código: " + codigo);
    }
}
