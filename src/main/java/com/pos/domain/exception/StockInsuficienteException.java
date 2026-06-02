package com.pos.domain.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String nombreProducto, int cantidadSolicitada, int stockDisponible) {
        super("El producto '" + nombreProducto + "' tiene solo " + stockDisponible
            + " unidades disponibles, se solicitaron " + cantidadSolicitada);
    }
}
