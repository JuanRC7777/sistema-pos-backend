package com.pos.domain;

import com.pos.domain.exception.StockInsuficienteException;
import com.pos.domain.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductoTest {

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId("1");
        producto.setNombre("Bebida Cola");
        producto.setCodigo("BEB-001");
        producto.setPrecio(new BigDecimal("1.50"));
        producto.setStock(10);
        producto.setActivo(true);
    }

    @Test
    void tieneStockSuficiente_cuandoStockEsMayor_retornaTrue() {
        assertThat(producto.tieneStockSuficiente(5)).isTrue();
    }

    @Test
    void tieneStockSuficiente_cuandoStockEsIgual_retornaTrue() {
        assertThat(producto.tieneStockSuficiente(10)).isTrue();
    }

    @Test
    void tieneStockSuficiente_cuandoStockEsMenor_retornaFalse() {
        assertThat(producto.tieneStockSuficiente(11)).isFalse();
    }

    @Test
    void descontarStock_cuandoStockEsSuficiente_reduceCantidad() {
        producto.descontarStock(3);
        assertThat(producto.getStock()).isEqualTo(7);
    }

    @Test
    void descontarStock_cuandoStockEsInsuficiente_lanzaExcepcion() {
        assertThatThrownBy(() -> producto.descontarStock(15))
            .isInstanceOf(StockInsuficienteException.class)
            .hasMessageContaining("Bebida Cola")
            .hasMessageContaining("10");
    }
}
