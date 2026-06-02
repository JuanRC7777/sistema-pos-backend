package com.pos.application.port.out;

import com.pos.application.dto.response.VentaResponse;
import com.pos.domain.model.DetalleVenta;
import com.pos.domain.model.Venta;
import java.util.List;

public interface VentaRepositoryPort {
    VentaResponse save(Venta venta, List<DetalleVenta> detalles);
}
