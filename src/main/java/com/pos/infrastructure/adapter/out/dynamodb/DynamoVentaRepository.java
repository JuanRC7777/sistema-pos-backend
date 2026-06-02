package com.pos.infrastructure.adapter.out.dynamodb;

import com.pos.application.dto.response.DetalleVentaResponse;
import com.pos.application.dto.response.VentaResponse;
import com.pos.application.port.out.VentaRepositoryPort;
import com.pos.domain.model.DetalleVenta;
import com.pos.domain.model.Venta;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.*;

public class DynamoVentaRepository implements VentaRepositoryPort {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoVentaRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = System.getenv("TABLA_VENTAS"); // <-- tabla separada
    }

    @Override
    public VentaResponse save(Venta venta, List<DetalleVenta> detalles) {
        List<TransactWriteItem> items = new ArrayList<>();

        Map<String, AttributeValue> ventaItem = new HashMap<>();
        ventaItem.put("PK", AttributeValue.fromS("VENTA#" + venta.getId()));
        ventaItem.put("SK", AttributeValue.fromS("METADATA"));
        ventaItem.put("id", AttributeValue.fromS(venta.getId()));
        ventaItem.put("numeroFactura", AttributeValue.fromS(venta.getNumeroFactura()));
        ventaItem.put("nombreCajero", AttributeValue.fromS(venta.getNombreCajero()));
        ventaItem.put("metodoPago", AttributeValue.fromS(venta.getMetodoPago()));
        ventaItem.put("subtotal", AttributeValue.fromN(venta.getSubtotal().toPlainString()));
        ventaItem.put("tasaImpuesto", AttributeValue.fromN(venta.getTasaImpuesto().toPlainString()));
        ventaItem.put("impuesto", AttributeValue.fromN(venta.getImpuesto().toPlainString()));
        ventaItem.put("total", AttributeValue.fromN(venta.getTotal().toPlainString()));
        ventaItem.put("fecha", AttributeValue.fromS(venta.getFecha()));
        if (venta.getNombreCliente() != null)
            ventaItem.put("nombreCliente", AttributeValue.fromS(venta.getNombreCliente()));
        if (venta.getCedulaCliente() != null)
            ventaItem.put("cedulaCliente", AttributeValue.fromS(venta.getCedulaCliente()));
        if (venta.getMontoPagado() != null)
            ventaItem.put("montoPagado", AttributeValue.fromN(venta.getMontoPagado().toPlainString()));
        if (venta.getCambio() != null)
            ventaItem.put("cambio", AttributeValue.fromN(venta.getCambio().toPlainString()));

        // Guardar venta principal
        items.add(TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(tableName)
                .item(ventaItem)
                .conditionExpression("attribute_not_exists(PK)")
                .build())
            .build());

        // Índice de factura — garantiza unicidad
        items.add(TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(tableName)
                .item(Map.of(
                    "PK", AttributeValue.fromS("FACTURA#" + venta.getNumeroFactura()),
                    "SK", AttributeValue.fromS("METADATA"),
                    "ventaId", AttributeValue.fromS(venta.getId())
                ))
                .conditionExpression("attribute_not_exists(PK)")
                .build())
            .build());

        // Detalles de la venta
        for (DetalleVenta detalle : detalles) {
            Map<String, AttributeValue> detalleItem = new HashMap<>();
            detalleItem.put("PK", AttributeValue.fromS("VENTA#" + venta.getId()));
            detalleItem.put("SK", AttributeValue.fromS("DETALLE#" + detalle.getProductoId()));
            detalleItem.put("codigoProducto", AttributeValue.fromS(detalle.getCodigoProducto()));
            detalleItem.put("nombreProducto", AttributeValue.fromS(detalle.getNombreProducto()));
            detalleItem.put("cantidad", AttributeValue.fromN(String.valueOf(detalle.getCantidad())));
            detalleItem.put("precioUnitario", AttributeValue.fromN(detalle.getPrecioUnitario().toPlainString()));
            detalleItem.put("subtotal", AttributeValue.fromN(detalle.getSubtotal().toPlainString()));
            items.add(TransactWriteItem.builder()
                .put(Put.builder().tableName(tableName).item(detalleItem).build())
                .build());
        }

        dynamoDbClient.transactWriteItems(
            TransactWriteItemsRequest.builder().transactItems(items).build()
        );

        return toVentaResponse(venta, detalles);
    }

    private VentaResponse toVentaResponse(Venta venta, List<DetalleVenta> detalles) {
        VentaResponse res = new VentaResponse();
        res.setId(venta.getId());
        res.setNumeroFactura(venta.getNumeroFactura());
        res.setFecha(venta.getFecha());
        res.setNombreCajero(venta.getNombreCajero());
        res.setNombreCliente(venta.getNombreCliente());
        res.setCedulaCliente(venta.getCedulaCliente());
        res.setMetodoPago(venta.getMetodoPago());
        res.setSubtotal(venta.getSubtotal());
        res.setTasaImpuesto(venta.getTasaImpuesto());
        res.setImpuesto(venta.getImpuesto());
        res.setTotal(venta.getTotal());
        res.setMontoPagado(venta.getMontoPagado());
        res.setCambio(venta.getCambio());
        res.setDetalles(detalles.stream().map(d -> {
            DetalleVentaResponse dr = new DetalleVentaResponse();
            dr.setCodigoProducto(d.getCodigoProducto());
            dr.setNombreProducto(d.getNombreProducto());
            dr.setCantidad(d.getCantidad());
            dr.setPrecioUnitario(d.getPrecioUnitario());
            dr.setSubtotal(d.getSubtotal());
            return dr;
        }).toList());
        return res;
    }
}
