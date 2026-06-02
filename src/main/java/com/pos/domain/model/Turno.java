package com.pos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Turno {

    private String id;
    private String nombreCajero;
    private BigDecimal saldoInicial;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalTransferencia;
    private int cantidadVentas;
    private String fechaApertura;
    private String fechaCierre;
    private String estado;
    private BigDecimal efectivoContado;
    private BigDecimal efectivoEsperado;
    private BigDecimal diferencia;
    private String resultadoCierre;

    public Turno() {}

    public void calcularCierre(BigDecimal efectivoContado) {
        this.efectivoContado = efectivoContado;
        this.efectivoEsperado = saldoInicial.add(totalEfectivo).setScale(2, RoundingMode.HALF_UP);
        this.diferencia = efectivoContado.subtract(efectivoEsperado).setScale(2, RoundingMode.HALF_UP);

        int cmp = this.diferencia.compareTo(BigDecimal.ZERO);
        if (cmp == 0)       this.resultadoCierre = "CUADRADO";
        else if (cmp > 0)   this.resultadoCierre = "SOBRANTE";
        else                this.resultadoCierre = "FALTANTE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombreCajero() { return nombreCajero; }
    public void setNombreCajero(String nombreCajero) { this.nombreCajero = nombreCajero; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }

    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(BigDecimal totalEfectivo) { this.totalEfectivo = totalEfectivo; }

    public BigDecimal getTotalTarjeta() { return totalTarjeta; }
    public void setTotalTarjeta(BigDecimal totalTarjeta) { this.totalTarjeta = totalTarjeta; }

    public BigDecimal getTotalTransferencia() { return totalTransferencia; }
    public void setTotalTransferencia(BigDecimal totalTransferencia) { this.totalTransferencia = totalTransferencia; }

    public int getCantidadVentas() { return cantidadVentas; }
    public void setCantidadVentas(int cantidadVentas) { this.cantidadVentas = cantidadVentas; }

    public String getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(String fechaApertura) { this.fechaApertura = fechaApertura; }

    public String getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(String fechaCierre) { this.fechaCierre = fechaCierre; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }

    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }

    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }

    public String getResultadoCierre() { return resultadoCierre; }
    public void setResultadoCierre(String resultadoCierre) { this.resultadoCierre = resultadoCierre; }
}
