package com.pos.application.service;

import com.pos.application.dto.response.ProductoResponse;
import com.pos.application.port.out.ProductoRepositoryPort;
import com.pos.domain.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductoServiceTest {

    private ProductoRepositoryPort productoRepo;
    private ProductoService service;

    @BeforeEach
    void setUp() {
        productoRepo = mock(ProductoRepositoryPort.class);
        service = new ProductoService(productoRepo);
    }

    @Test
    void listar_hayProductos_retornaLista() {
        Producto p = new Producto();
        p.setId("1"); p.setCodigo("BEB-001"); p.setNombre("Bebida");
        p.setPrecio(new BigDecimal("1.50")); p.setStock(10);

        when(productoRepo.findAllActivos()).thenReturn(List.of(p));

        List<ProductoResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("BEB-001");
    }

    @Test
    void listar_sinProductos_retornaListaVacia() {
        when(productoRepo.findAllActivos()).thenReturn(List.of());
        assertThat(service.listar()).isEmpty();
    }
}
