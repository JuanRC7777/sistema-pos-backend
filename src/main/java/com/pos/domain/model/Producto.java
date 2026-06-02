package com.pos.domain.model;

import com.pos.domain.exception.StockInsuficienteException;
import java.math.BigDecimal;

public class Producto {

    private String id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private int stock;
    private boolean activo;

    public Producto() {}

    public boolean tieneStockSuficiente(int cantidad) {
        return this.stock >= cantidad;
    }

    public void descontarStock(int cantidad) {
        if (!tieneStockSuficiente(cantidad)) {
            throw new StockInsuficienteException(this.nombre, cantidad, this.stock);
        }
        this.stock -= cantidad;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
