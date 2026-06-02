package com.pos.infrastructure.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.application.port.in.producto.ListarProductosUseCase;
import com.pos.application.service.ProductoService;
import com.pos.infrastructure.adapter.out.dynamodb.DynamoProductoRepository;
import com.pos.infrastructure.config.DynamoConfig;

import java.util.List;

public class ListarProductosFunction implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final ListarProductosUseCase listarProductosUseCase;
    private final ObjectMapper objectMapper;

    public ListarProductosFunction() {
        this.listarProductosUseCase = new ProductoService(
            new DynamoProductoRepository(DynamoConfig.buildClient())
        );
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            List<?> productos = listarProductosUseCase.listar();
            return ApiGatewayResponse.ok200(objectMapper.writeValueAsString(productos));
        } catch (Exception e) {
            context.getLogger().log("Error en ListarProductosFunction: " + e.getMessage());
            return ApiGatewayResponse.error(500, "Error interno del servidor");
        }
    }
}
