package com.pos.application.dto.command;

import java.math.BigDecimal;

public class CerrarTurnoCommand {
    private BigDecimal efectivoContado;

    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
}
