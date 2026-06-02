package com.pos.infrastructure.adapter.out.dynamodb;

import com.pos.application.dto.response.TurnoResponse;
import com.pos.application.port.out.TurnoRepositoryPort;
import com.pos.domain.model.Turno;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DynamoTurnoRepository implements TurnoRepositoryPort {

    private static final String PK_ACTIVO = "TURNO#ACTIVO";
    private static final String SK = "METADATA";

    private final DynamoDbClient client;
    private final String tableName;

    public DynamoTurnoRepository(DynamoDbClient client) {
        this.tableName = System.getenv("TABLA_VENTAS");
        this.client = client;
    }

    @Override
    public void abrir(Turno turno) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.fromS(PK_ACTIVO));
        item.put("SK", AttributeValue.fromS(SK));
        item.put("id", AttributeValue.fromS(turno.getId()));
        item.put("nombreCajero", AttributeValue.fromS(turno.getNombreCajero()));
        item.put("saldoInicial", AttributeValue.fromN(turno.getSaldoInicial().toPlainString()));
        item.put("totalEfectivo", AttributeValue.fromN("0"));
        item.put("totalTarjeta", AttributeValue.fromN("0"));
        item.put("totalTransferencia", AttributeValue.fromN("0"));
        item.put("cantidadVentas", AttributeValue.fromN("0"));
        item.put("fechaApertura", AttributeValue.fromS(turno.getFechaApertura()));
        item.put("estado", AttributeValue.fromS("ABIERTO"));

        client.putItem(PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .conditionExpression("attribute_not_exists(PK)")
            .build());
    }

    @Override
    public Optional<Turno> obtenerActivo() {
        GetItemResponse response = client.getItem(GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of(
                "PK", AttributeValue.fromS(PK_ACTIVO),
                "SK", AttributeValue.fromS(SK)
            ))
            .build());

        if (!response.hasItem()) return Optional.empty();
        return Optional.of(itemToTurno(response.item()));
    }

    @Override
    public TurnoResponse cerrar(Turno turno) {
        // Leer totales actuales del turno activo
        GetItemResponse actual = client.getItem(GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of(
                "PK", AttributeValue.fromS(PK_ACTIVO),
                "SK", AttributeValue.fromS(SK)
            ))
            .build());

        Turno conTotales = itemToTurno(actual.item());
        turno.setTotalEfectivo(conTotales.getTotalEfectivo());
        turno.setTotalTarjeta(conTotales.getTotalTarjeta());
        turno.setTotalTransferencia(conTotales.getTotalTransferencia());
        turno.setCantidadVentas(conTotales.getCantidadVentas());
        turno.calcularCierre(turno.getEfectivoContado());

        // Guardar turno cerrado con su ID histórico
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.fromS("TURNO#" + turno.getId()));
        item.put("SK", AttributeValue.fromS(SK));
        item.put("id", AttributeValue.fromS(turno.getId()));
        item.put("nombreCajero", AttributeValue.fromS(turno.getNombreCajero()));
        item.put("saldoInicial", AttributeValue.fromN(turno.getSaldoInicial().toPlainString()));
        item.put("totalEfectivo", AttributeValue.fromN(turno.getTotalEfectivo().toPlainString()));
        item.put("totalTarjeta", AttributeValue.fromN(turno.getTotalTarjeta().toPlainString()));
        item.put("totalTransferencia", AttributeValue.fromN(turno.getTotalTransferencia().toPlainString()));
        item.put("cantidadVentas", AttributeValue.fromN(String.valueOf(turno.getCantidadVentas())));
        item.put("fechaApertura", AttributeValue.fromS(turno.getFechaApertura()));
        item.put("fechaCierre", AttributeValue.fromS(turno.getFechaCierre()));
        item.put("estado", AttributeValue.fromS("CERRADO"));
        item.put("efectivoContado", AttributeValue.fromN(turno.getEfectivoContado().toPlainString()));
        item.put("efectivoEsperado", AttributeValue.fromN(turno.getEfectivoEsperado().toPlainString()));
        item.put("diferencia", AttributeValue.fromN(turno.getDiferencia().toPlainString()));
        item.put("resultadoCierre", AttributeValue.fromS(turno.getResultadoCierre()));

        client.transactWriteItems(TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .put(Put.builder().tableName(tableName).item(item).build())
                    .build(),
                TransactWriteItem.builder()
                    .delete(Delete.builder()
                        .tableName(tableName)
                        .key(Map.of(
                            "PK", AttributeValue.fromS(PK_ACTIVO),
                            "SK", AttributeValue.fromS(SK)
                        ))
                        .build())
                    .build()
            )
            .build());

        return toResponse(turno);
    }

    @Override
    public void acumularVenta(String metodoPago, BigDecimal total) {
        String campo = switch (metodoPago) {
            case "EFECTIVO"      -> "totalEfectivo";
            case "TARJETA"       -> "totalTarjeta";
            case "TRANSFERENCIA" -> "totalTransferencia";
            default              -> null;
        };
        if (campo == null) return;

        try {
            client.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                    "PK", AttributeValue.fromS(PK_ACTIVO),
                    "SK", AttributeValue.fromS(SK)
                ))
                .updateExpression("ADD " + campo + " :total, cantidadVentas :uno")
                .conditionExpression("attribute_exists(PK)")
                .expressionAttributeValues(Map.of(
                    ":total", AttributeValue.fromN(total.toPlainString()),
                    ":uno",   AttributeValue.fromN("1")
                ))
                .build());
        } catch (ConditionalCheckFailedException ignored) {
            // No hay turno activo — la venta se registra igual
        }
    }

    private Turno itemToTurno(Map<String, AttributeValue> item) {
        Turno t = new Turno();
        t.setId(item.get("id").s());
        t.setNombreCajero(item.get("nombreCajero").s());
        t.setSaldoInicial(new BigDecimal(item.get("saldoInicial").n()));
        t.setTotalEfectivo(new BigDecimal(item.get("totalEfectivo").n()));
        t.setTotalTarjeta(new BigDecimal(item.get("totalTarjeta").n()));
        t.setTotalTransferencia(new BigDecimal(item.get("totalTransferencia").n()));
        t.setCantidadVentas(Integer.parseInt(item.get("cantidadVentas").n()));
        t.setFechaApertura(item.get("fechaApertura").s());
        t.setEstado(item.get("estado").s());
        return t;
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
        r.setEfectivoEsperado(t.getEfectivoEsperado());
        r.setFechaApertura(t.getFechaApertura());
        r.setFechaCierre(t.getFechaCierre());
        r.setEstado(t.getEstado());
        r.setEfectivoContado(t.getEfectivoContado());
        r.setDiferencia(t.getDiferencia());
        r.setResultadoCierre(t.getResultadoCierre());
        return r;
    }
}
