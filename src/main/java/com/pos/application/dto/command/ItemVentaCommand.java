package com.pos.application.dto.command;

public class ItemVentaCommand {
    private String codigoProducto;
    private int cantidad;

    public ItemVentaCommand() {}

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
