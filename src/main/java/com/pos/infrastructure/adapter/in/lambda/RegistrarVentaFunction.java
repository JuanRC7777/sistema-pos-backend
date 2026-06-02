package com.pos.infrastructure.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.application.dto.command.RegistrarVentaCommand;
import com.pos.application.dto.response.VentaResponse;
import com.pos.application.port.in.venta.RegistrarVentaUseCase;
import com.pos.application.service.RegistrarVentaService;
import com.pos.domain.exception.LimiteFacturasDiarioExcedidoException;
import com.pos.domain.exception.ProductoNoEncontradoException;
import com.pos.domain.exception.StockInsuficienteException;
import com.pos.domain.service.GeneradorNumeroFactura;
import com.pos.infrastructure.adapter.out.dynamodb.DynamoProductoRepository;
import com.pos.infrastructure.adapter.out.dynamodb.DynamoSecuenciaRepository;
import com.pos.infrastructure.adapter.out.dynamodb.DynamoTurnoRepository;
import com.pos.infrastructure.adapter.out.dynamodb.DynamoVentaRepository;
import com.pos.infrastructure.config.DynamoConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.math.BigDecimal;

public class RegistrarVentaFunction implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final RegistrarVentaUseCase registrarVentaUseCase;
    private final ObjectMapper objectMapper;

    public RegistrarVentaFunction() {
        DynamoDbClient client = DynamoConfig.buildClient();
        BigDecimal tasaImpuesto = new BigDecimal(System.getenv("TASA_IMPUESTO"));

        this.registrarVentaUseCase = new RegistrarVentaService(
            new DynamoProductoRepository(client),
            new DynamoVentaRepository(client),
            new DynamoSecuenciaRepository(client),
            new DynamoTurnoRepository(client),
            new GeneradorNumeroFactura(),
            tasaImpuesto
        );
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String username = event.getRequestContext()
                .getAuthorizer()
                .getJwt()
                .getClaims()
                .get("cognito:username");

            RegistrarVentaCommand cmd = objectMapper.readValue(
                event.getBody(), RegistrarVentaCommand.class);
            cmd.setUsernameCajero(username);

            VentaResponse response = registrarVentaUseCase.registrar(cmd);
            return ApiGatewayResponse.created201(objectMapper.writeValueAsString(response));

        } catch (ProductoNoEncontradoException e) {
            return ApiGatewayResponse.error(404, e.getMessage());
        } catch (StockInsuficienteException e) {
            return ApiGatewayResponse.error(422, e.getMessage());
        } catch (LimiteFacturasDiarioExcedidoException e) {
            return ApiGatewayResponse.error(409, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiGatewayResponse.error(400, e.getMessage());
        } catch (Exception e) {
            context.getLogger().log("Error en RegistrarVentaFunction: " + e.getMessage());
            return ApiGatewayResponse.error(500, "Error interno del servidor");
        }
    }
}
