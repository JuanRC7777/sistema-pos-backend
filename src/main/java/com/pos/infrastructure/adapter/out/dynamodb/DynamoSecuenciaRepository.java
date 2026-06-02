package com.pos.infrastructure.adapter.out.dynamodb;

import com.pos.application.port.out.SecuenciaFacturaRepositoryPort;
import com.pos.domain.exception.LimiteFacturasDiarioExcedidoException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DynamoSecuenciaRepository implements SecuenciaFacturaRepositoryPort {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int LIMITE_DIARIO = 999999;

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoSecuenciaRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = System.getenv("TABLA_VENTAS"); // la secuencia vive en la tabla de ventas
    }

    @Override
    public int obtenerYIncrementarSecuencia(LocalDate fecha) {
        try {
            UpdateItemResponse response = dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                    "PK", AttributeValue.fromS("SEQ#" + fecha.format(FORMATO)),
                    "SK", AttributeValue.fromS("METADATA")
                ))
                .updateExpression("ADD ultimoNumero :uno")
                .conditionExpression(
                    "attribute_not_exists(ultimoNumero) OR ultimoNumero < :limite")
                .expressionAttributeValues(Map.of(
                    ":uno", AttributeValue.fromN("1"),
                    ":limite", AttributeValue.fromN(String.valueOf(LIMITE_DIARIO))
                ))
                .returnValues(ReturnValue.ALL_NEW)
                .build());

            return Integer.parseInt(response.attributes().get("ultimoNumero").n());

        } catch (ConditionalCheckFailedException e) {
            throw new LimiteFacturasDiarioExcedidoException();
        }
    }
}
