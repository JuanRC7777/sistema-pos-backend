package com.pos.application.service;

import com.pos.application.dto.response.ProductoResponse;
import com.pos.application.port.in.producto.ListarProductosUseCase;
import com.pos.application.port.out.ProductoRepositoryPort;
import com.pos.domain.model.Producto;

import java.util.List;

public class ProductoService implements ListarProductosUseCase {

    private final ProductoRepositoryPort productoRepo;

    public ProductoService(ProductoRepositoryPort productoRepo) {
        this.productoRepo = productoRepo;
    }

    @Override
    public List<ProductoResponse> listar() {
        return productoRepo.findAllActivos()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private ProductoResponse toResponse(Producto p) {
        ProductoResponse res = new ProductoResponse();
        res.setId(p.getId());
        res.setCodigo(p.getCodigo());
        res.setNombre(p.getNombre());
        res.setDescripcion(p.getDescripcion());
        res.setPrecio(p.getPrecio());
        res.setStock(p.getStock());
        return res;
    }
}
