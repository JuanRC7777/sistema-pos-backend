package com.pos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Venta {

    private String id;
    private String numeroFactura;
    private String nombreCajero;
    private String nombreCliente;   // nullable
    private String cedulaCliente;  // nullable
    private List<DetalleVenta> detalles;
    private String metodoPago;
    private BigDecimal subtotal;
    private BigDecimal tasaImpuesto;
    private BigDecimal impuesto;
    private BigDecimal total;
    private BigDecimal montoPagado; // nullable — solo EFECTIVO
    private BigDecimal cambio;      // nullable — solo EFECTIVO
    private String fecha;

    public Venta() {}

    public void calcularTotales() {
        this.subtotal = detalles.stream()
            .map(DetalleVenta::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.impuesto = subtotal
            .multiply(tasaImpuesto)
            .setScale(2, RoundingMode.HALF_UP);

        this.total = subtotal.add(impuesto)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public void calcularCambio() {
        if (montoPagado != null) {
            this.cambio = montoPagado.subtract(total).setScale(2, RoundingMode.HALF_UP);
        }
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public String getNombreCajero() { return nombreCajero; }
    public void setNombreCajero(String nombreCajero) { this.nombreCajero = nombreCajero; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getCedulaCliente() { return cedulaCliente; }
    public void setCedulaCliente(String cedulaCliente) { this.cedulaCliente = cedulaCliente; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTasaImpuesto() { return tasaImpuesto; }
    public void setTasaImpuesto(BigDecimal tasaImpuesto) { this.tasaImpuesto = tasaImpuesto; }

    public BigDecimal getImpuesto() { return impuesto; }
    public void setImpuesto(BigDecimal impuesto) { this.impuesto = impuesto; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }

    public BigDecimal getCambio() { return cambio; }
    public void setCambio(BigDecimal cambio) { this.cambio = cambio; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}
