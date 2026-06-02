package com.pos.application.port.out;

import com.pos.domain.model.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositoryPort {
    Optional<Producto> findByCodigo(String codigo);
    List<Producto> findAllActivos();
    void decrementarStock(String productoId, int cantidad);
}
