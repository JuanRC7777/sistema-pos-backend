package com.pos.domain;

import com.pos.domain.model.DetalleVenta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DetalleVentaTest {

    @Test
    void calcularSubtotal_multiplicaPrecioYCantidad() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setPrecioUnitario(new BigDecimal("1.50"));
        detalle.setCantidad(2);

        BigDecimal resultado = detalle.calcularSubtotal();

        assertThat(resultado).isEqualByComparingTo("3.00");
    }

    @Test
    void calcularSubtotal_redondeoHalfUp() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setPrecioUnitario(new BigDecimal("1.005"));
        detalle.setCantidad(1);

        BigDecimal resultado = detalle.calcularSubtotal();

        assertThat(resultado).isEqualByComparingTo("1.01");
    }

    @Test
    void calcularSubtotal_cantidadUno_retornaPrecio() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setPrecioUnitario(new BigDecimal("0.75"));
        detalle.setCantidad(1);

        BigDecimal resultado = detalle.calcularSubtotal();

        assertThat(resultado).isEqualByComparingTo("0.75");
    }
}
