package com.pos.application.service;

import com.pos.application.dto.command.AbrirTurnoCommand;
import com.pos.application.dto.command.CerrarTurnoCommand;
import com.pos.application.dto.response.TurnoResponse;
import com.pos.application.port.in.caja.AbrirTurnoUseCase;
import com.pos.application.port.in.caja.CerrarTurnoUseCase;
import com.pos.application.port.in.caja.ConsultarTurnoUseCase;
import com.pos.application.port.out.TurnoRepositoryPort;
import com.pos.domain.exception.TurnoNoEncontradoException;
import com.pos.domain.exception.TurnoYaAbiertoException;
import com.pos.domain.model.Turno;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TurnoService implements AbrirTurnoUseCase, CerrarTurnoUseCase, ConsultarTurnoUseCase {

    private final TurnoRepositoryPort turnoRepo;

    public TurnoService(TurnoRepositoryPort turnoRepo) {
        this.turnoRepo = turnoRepo;
    }

    @Override
    public TurnoResponse abrir(AbrirTurnoCommand cmd) {
        if (cmd.getSaldoInicial() == null || cmd.getSaldoInicial().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }

        turnoRepo.obtenerActivo().ifPresent(t -> { throw new TurnoYaAbiertoException(); });

        Turno turno = new Turno();
        turno.setId(UUID.randomUUID().toString());
        turno.setNombreCajero(cmd.getUsernameCajero());
        turno.setSaldoInicial(cmd.getSaldoInicial());
        turno.setTotalEfectivo(BigDecimal.ZERO);
        turno.setTotalTarjeta(BigDecimal.ZERO);
        turno.setTotalTransferencia(BigDecimal.ZERO);
        turno.setCantidadVentas(0);
        turno.setFechaApertura(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        turno.setEstado("ABIERTO");

        turnoRepo.abrir(turno);
        return toResponse(turno);
    }

    @Override
    public TurnoResponse consultar() {
        Turno turno = turnoRepo.obtenerActivo()
            .orElseThrow(TurnoNoEncontradoException::new);
        return toResponse(turno);
    }

    @Override
    public TurnoResponse cerrar(CerrarTurnoCommand cmd) {
        if (cmd.getEfectivoContado() == null || cmd.getEfectivoContado().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El efectivo contado no puede ser negativo.");
        }

        Turno turno = turnoRepo.obtenerActivo()
            .orElseThrow(TurnoNoEncontradoException::new);

        turno.calcularCierre(cmd.getEfectivoContado());
        turno.setFechaCierre(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        turno.setEstado("CERRADO");

        return turnoRepo.cerrar(turno);
    }

    private TurnoResponse toResponse(Turno t) {
        TurnoResponse r = new TurnoResponse();
        r.setId(t.getId());
        r.setNombreCajero(t.getNombreCajero());
        r.setSaldoInicial(t.getSaldoInicial());
        r.setTotalEfectivo(t.getTotalEfectivo());
        r.setTotalTarjeta(t.getTotalTarjeta());
        r.setTotalTransferencia(t.getTotalTransferencia());
        r.setTotalVentas(t.getTotalEfectivo().add(t.getTotalTarjeta()).add(t.getTotalTransferencia()));
        r.setCantidadVentas(t.getCantidadVentas());
        r.setEfectivoEsperado(t.getSaldoInicial().add(t.getTotalEfectivo()));
        r.setFechaApertura(t.getFechaApertura());
        r.setFechaCierre(t.getFechaCierre());
        r.setEstado(t.getEstado());
        r.setEfectivoContado(t.getEfectivoContado());
        r.setDiferencia(t.getDiferencia());
        r.setResultadoCierre(t.getResultadoCierre());
        return r;
    }
}
