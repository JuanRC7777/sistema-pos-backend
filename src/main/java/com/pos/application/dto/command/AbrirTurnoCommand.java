package com.pos.application.dto.command;

import java.math.BigDecimal;

public class AbrirTurnoCommand {
    private String usernameCajero;
    private BigDecimal saldoInicial;

    public String getUsernameCajero() { return usernameCajero; }
    public void setUsernameCajero(String usernameCajero) { this.usernameCajero = usernameCajero; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
}
