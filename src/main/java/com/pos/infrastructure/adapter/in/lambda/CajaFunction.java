package com.pos.infrastructure.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.application.dto.command.AbrirTurnoCommand;
import com.pos.application.dto.command.CerrarTurnoCommand;
import com.pos.application.dto.response.TurnoResponse;
import com.pos.application.service.TurnoService;
import com.pos.domain.exception.TurnoNoEncontradoException;
import com.pos.domain.exception.TurnoYaAbiertoException;
import com.pos.infrastructure.adapter.out.dynamodb.DynamoTurnoRepository;
import com.pos.infrastructure.config.DynamoConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class CajaFunction implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final TurnoService turnoService;
    private final ObjectMapper objectMapper;

    public CajaFunction() {
        DynamoDbClient client = DynamoConfig.buildClient();
        this.turnoService = new TurnoService(new DynamoTurnoRepository(client));
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String method = event.getRequestContext().getHttp().getMethod();
            String path   = event.getRequestContext().getHttp().getPath();

            String username = event.getRequestContext()
                .getAuthorizer().getJwt().getClaims().get("cognito:username");

            if (method.equals("POST") && path.endsWith("/abrir")) {
                AbrirTurnoCommand cmd = objectMapper.readValue(event.getBody(), AbrirTurnoCommand.class);
                cmd.setUsernameCajero(username);
                TurnoResponse res = turnoService.abrir(cmd);
                return ApiGatewayResponse.created201(objectMapper.writeValueAsString(res));
            }

            if (method.equals("GET") && path.endsWith("/turno-actual")) {
                TurnoResponse res = turnoService.consultar();
                return ApiGatewayResponse.ok200(objectMapper.writeValueAsString(res));
            }

            if (method.equals("POST") && path.endsWith("/cerrar")) {
                CerrarTurnoCommand cmd = objectMapper.readValue(event.getBody(), CerrarTurnoCommand.class);
                TurnoResponse res = turnoService.cerrar(cmd);
                return ApiGatewayResponse.ok200(objectMapper.writeValueAsString(res));
            }

            return ApiGatewayResponse.error(404, "Ruta no encontrada");

        } catch (TurnoYaAbiertoException e) {
            return ApiGatewayResponse.error(409, e.getMessage());
        } catch (TurnoNoEncontradoException e) {
            return ApiGatewayResponse.error(404, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiGatewayResponse.error(400, e.getMessage());
        } catch (Exception e) {
            context.getLogger().log("Error en CajaFunction: " + e.getMessage());
            return ApiGatewayResponse.error(500, "Error interno del servidor");
        }
    }
}
