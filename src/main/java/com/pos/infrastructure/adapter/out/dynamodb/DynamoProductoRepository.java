package com.pos.infrastructure.adapter.out.dynamodb;

import com.pos.application.port.out.ProductoRepositoryPort;
import com.pos.domain.exception.StockInsuficienteException;
import com.pos.domain.model.Producto;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.*;

public class DynamoProductoRepository implements ProductoRepositoryPort {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoProductoRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = System.getenv("TABLA_PRODUCTOS"); // <-- tabla separada
    }

    @Override
    public Optional<Producto> findByCodigo(String codigo) {
        GetItemResponse codigoItem = dynamoDbClient.getItem(GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of(
                "PK", AttributeValue.fromS("CODIGO#" + codigo),
                "SK", AttributeValue.fromS("METADATA")
            ))
            .build());

        if (!codigoItem.hasItem()) return Optional.empty();

        String productoId = codigoItem.item().get("productoId").s();

        GetItemResponse productoItem = dynamoDbClient.getItem(GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of(
                "PK", AttributeValue.fromS("PROD#" + productoId),
                "SK", AttributeValue.fromS("METADATA")
            ))
            .build());

        if (!productoItem.hasItem()) return Optional.empty();

        return Optional.of(itemToProducto(productoId, productoItem.item()));
    }

    @Override
    public List<Producto> findAllActivos() {
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
            .tableName(tableName)
            .indexName("GSI1-activo-nombre")
            .keyConditionExpression("activo = :activo")
            .expressionAttributeValues(Map.of(
                ":activo", AttributeValue.fromS("true")
            ))
            .build());

        return response.items().stream()
            .map(item -> itemToProducto(item.get("productoId").s(), item))
            .toList();
    }

    @Override
    public void decrementarStock(String productoId, int cantidad) {
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                    "PK", AttributeValue.fromS("PROD#" + productoId),
                    "SK", AttributeValue.fromS("METADATA")
                ))
                .updateExpression("SET stock = stock - :cantidad")
                .conditionExpression("stock >= :cantidad")
                .expressionAttributeValues(Map.of(
                    ":cantidad", AttributeValue.fromN(String.valueOf(cantidad))
                ))
                .build());
        } catch (ConditionalCheckFailedException e) {
            throw new StockInsuficienteException("producto id=" + productoId, cantidad, 0);
        }
    }

    private Producto itemToProducto(String productoId, Map<String, AttributeValue> item) {
        Producto p = new Producto();
        p.setId(productoId);
        p.setCodigo(item.getOrDefault("codigo", AttributeValue.fromS("")).s());
        p.setNombre(item.getOrDefault("nombre", AttributeValue.fromS("")).s());
        p.setDescripcion(item.getOrDefault("descripcion", AttributeValue.fromS("")).s());
        p.setPrecio(new BigDecimal(item.getOrDefault("precio", AttributeValue.fromN("0")).n()));
        p.setStock(Integer.parseInt(item.getOrDefault("stock", AttributeValue.fromN("0")).n()));
        p.setActivo("true".equals(item.getOrDefault("activo", AttributeValue.fromS("false")).s()));
        return p;
    }
}
