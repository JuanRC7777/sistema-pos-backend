package com.pos.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class VentaResponse {
    private String id;
    private String numeroFactura;
    private String fecha;
    private String nombreCajero;
    private String nombreCliente;
    private String cedulaCliente;
    private List<DetalleVentaResponse> detalles;
    private String metodoPago;
    private BigDecimal subtotal;
    private BigDecimal tasaImpuesto;
    private BigDecimal impuesto;
    private BigDecimal total;
    private BigDecimal montoPagado;  // solo para EFECTIVO, null en otros métodos
    private BigDecimal cambio;       // solo para EFECTIVO, null en otros métodos

    public VentaResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getNombreCajero() { return nombreCajero; }
    public void setNombreCajero(String nombreCajero) { this.nombreCajero = nombreCajero; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getCedulaCliente() { return cedulaCliente; }
    public void setCedulaCliente(String cedulaCliente) { this.cedulaCliente = cedulaCliente; }

    public List<DetalleVentaResponse> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVentaResponse> detalles) { this.detalles = detalles; }

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
}
