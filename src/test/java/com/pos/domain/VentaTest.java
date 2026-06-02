package com.pos.domain;

import com.pos.domain.model.DetalleVenta;
import com.pos.domain.model.Venta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VentaTest {

    private DetalleVenta detalle(String precio, int cantidad) {
        DetalleVenta d = new DetalleVenta();
        d.setPrecioUnitario(new BigDecimal(precio));
        d.setCantidad(cantidad);
        return d;
    }

    @Test
    void calcularTotales_sumaSubtotalesCorrectamente() {
        Venta venta = new Venta();
        venta.setTasaImpuesto(new BigDecimal("0.05"));
        venta.setDetalles(List.of(
            detalle("1.50", 2),  // 3.00
            detalle("0.75", 1)   // 0.75
        ));

        venta.calcularTotales();

        assertThat(venta.getSubtotal()).isEqualByComparingTo("3.75");
    }

    @Test
    void calcularTotales_calculaImpuestoCorrectamente() {
        Venta venta = new Venta();
        venta.setTasaImpuesto(new BigDecimal("0.05"));
        venta.setDetalles(List.of(detalle("1.50", 2))); // subtotal = 3.00

        venta.calcularTotales();

        assertThat(venta.getImpuesto()).isEqualByComparingTo("0.15"); // 3.00 * 0.05
    }

    @Test
    void calcularTotales_calculaTotalCorrectamente() {
        Venta venta = new Venta();
        venta.setTasaImpuesto(new BigDecimal("0.05"));
        venta.setDetalles(List.of(detalle("1.50", 2))); // subtotal = 3.00

        venta.calcularTotales();

        assertThat(venta.getTotal()).isEqualByComparingTo("3.15"); // 3.00 + 0.15
    }

    @Test
    void calcularTotales_clienteNullNoLanzaError() {
        Venta venta = new Venta();
        venta.setTasaImpuesto(new BigDecimal("0.05"));
        venta.setNombreCliente(null);
        venta.setCedulaCliente(null);
        venta.setDetalles(List.of(detalle("1.00", 1)));

        venta.calcularTotales();

        assertThat(venta.getTotal()).isEqualByComparingTo("1.05");
    }
}
