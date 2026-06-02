package com.pos.application.dto.command;

import java.math.BigDecimal;
import java.util.List;

public class RegistrarVentaCommand {

    // Asignado desde el token JWT en el handler, no viene del body
    private String usernameCajero;

    private List<ItemVentaCommand> items;
    private String metodoPago;          // EFECTIVO | TARJETA | TRANSFERENCIA
    private BigDecimal montoPagado;     // Solo requerido si metodoPago = EFECTIVO
    private String nombreCliente;       // opcional
    private String cedulaCliente;       // opcional, 10 dígitos

    public RegistrarVentaCommand() {}

    public String getUsernameCajero() { return usernameCajero; }
    public void setUsernameCajero(String usernameCajero) { this.usernameCajero = usernameCajero; }

    public List<ItemVentaCommand> getItems() { return items; }
    public void setItems(List<ItemVentaCommand> items) { this.items = items; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getCedulaCliente() { return cedulaCliente; }
    public void setCedulaCliente(String cedulaCliente) { this.cedulaCliente = cedulaCliente; }
}
