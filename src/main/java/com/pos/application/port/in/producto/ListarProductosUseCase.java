package com.pos.application.port.in.producto;

import com.pos.application.dto.response.ProductoResponse;
import java.util.List;

public interface ListarProductosUseCase {
    List<ProductoResponse> listar();
}
