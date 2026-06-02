# Implementation Tasks: Sistema POS — Serverless AWS
**Versión:** 1.0.0
**Fecha:** 2026-05-25
**Estado:** Completado y desplegado en AWS us-east-1

---

## Resumen de lo implementado

| Wave | Descripción | Estado |
|---|---|---|
| 1 | Configuración Maven + estructura de carpetas | ✅ Completado |
| 2 | Modelos del dominio | ✅ Completado |
| 3 | GeneradorNumeroFactura | ✅ Completado |
| 4 | Tests del dominio | ✅ Completado |
| 5 | Ports (interfaces) | ✅ Completado |
| 6 | DTOs (Commands y Responses) | ✅ Completado |
| 7 | Servicios de aplicación | ✅ Completado |
| 8 | Tests de servicios | ✅ Completado |
| 9 | Lambda Handlers | ✅ Completado |
| 10 | Repositorios DynamoDB | ✅ Completado |
| 11 | Infraestructura SAM + despliegue | ✅ Desplegado |

---

## Wave 1 — Configuración del proyecto

- [x] **1.1** Crear proyecto Maven con `groupId: com.pos`, `artifactId: sistema-pos`, Java 21.
- [x] **1.2** Agregar dependencias en `pom.xml`:
  - `aws-lambda-java-core:1.2.3`
  - `aws-lambda-java-events:3.11.4`
  - `dynamodb-enhanced:2.25.60`
  - `cognitoidentityprovider:2.25.60`
  - `jackson-databind:2.17.1`
  - `jakarta.validation-api:3.0.2`
  - `junit-jupiter:5.10.2`
  - `mockito-core:5.11.0`
  - `assertj-core:3.25.3`
- [x] **1.3** Agregar `maven-shade-plugin:3.5.2` para generar JAR con todas las dependencias (requerido por Lambda).
- [x] **1.4** Crear estructura de paquetes:
  ```
  com.pos.domain.model
  com.pos.domain.exception
  com.pos.domain.service
  com.pos.application.port.in.auth
  com.pos.application.port.in.producto
  com.pos.application.port.in.venta
  com.pos.application.port.out
  com.pos.application.service
  com.pos.application.dto.command
  com.pos.application.dto.response
  com.pos.infrastructure.adapter.in.lambda
  com.pos.infrastructure.adapter.out.dynamodb
  com.pos.infrastructure.config
  ```
- [x] **1.5** Crear `template.yaml` base con estructura SAM.

---

## Wave 2 — Modelos del dominio

- [x] **2.1** Crear `Producto.java`:
  - Campos: `id`, `codigo`, `nombre`, `descripcion`, `precio` (BigDecimal), `stock` (int), `activo` (boolean)
  - `tieneStockSuficiente(int cantidad)`: retorna `stock >= cantidad`
  - `descontarStock(int cantidad)`: lanza `StockInsuficienteException` si insuficiente

- [x] **2.2** Crear `DetalleVenta.java`:
  - Campos: `ventaId`, `productoId`, `codigoProducto`, `nombreProducto`, `cantidad`, `precioUnitario`, `subtotal`
  - `calcularSubtotal()`: `precioUnitario × cantidad` con `ROUND_HALF_UP` a 2 decimales

- [x] **2.3** Crear `Venta.java`:
  - Campos: `id`, `numeroFactura`, `nombreCajero`, `nombreCliente` (nullable), `cedulaCliente` (nullable), `detalles`, `metodoPago`, `subtotal`, `tasaImpuesto`, `impuesto`, `total`, `montoPagado` (nullable), `cambio` (nullable), `fecha`
  - `calcularTotales()`: suma subtotales → calcula impuesto → calcula total, todo con `ROUND_HALF_UP`
  - `calcularCambio()`: `cambio = montoPagado - total`, `ROUND_HALF_UP`

- [x] **2.4** Crear excepciones en `domain/exception/`:
  - `StockInsuficienteException(String nombreProducto, int cantidadSolicitada, int stockDisponible)`
  - `ProductoNoEncontradoException(String codigo)`
  - `VentaNoEncontradaException(String identificador)`
  - `CredencialesInvalidasException()`
  - `LimiteFacturasDiarioExcedidoException()`

---

## Wave 3 — Servicio de dominio

- [x] **3.1** Crear `GeneradorNumeroFactura.java`:
  - `generar(LocalDate fecha, int secuencia)`: retorna `FAC-YYYYMMDD-NNNNNN` con `String.format("FAC-%s-%06d", ...)`
  - `validarFormato(String numeroFactura)`: valida regex `^FAC-\d{8}-\d{6}$`

---

## Wave 4 — Tests del dominio

- [x] **4.1** `ProductoTest` — 5 tests:
  - `tieneStockSuficiente` retorna `true` cuando `stock >= cantidad`
  - `tieneStockSuficiente` retorna `true` cuando stock es igual
  - `tieneStockSuficiente` retorna `false` cuando `stock < cantidad`
  - `descontarStock` reduce el stock correctamente
  - `descontarStock` lanza `StockInsuficienteException` con nombre del producto en el mensaje

- [x] **4.2** `DetalleVentaTest` — 3 tests:
  - `calcularSubtotal` multiplica precio por cantidad correctamente
  - `calcularSubtotal` aplica `ROUND_HALF_UP` (1.005 × 1 = 1.01)
  - `calcularSubtotal` con cantidad 1 retorna el precio

- [x] **4.3** `VentaTest` — 4 tests:
  - `calcularTotales` suma subtotales correctamente
  - `calcularTotales` calcula impuesto con `ROUND_HALF_UP`
  - `calcularTotales` calcula total correctamente
  - Venta con `nombreCliente` y `cedulaCliente` null no lanza error

- [x] **4.4** `GeneradorNumeroFacturaTest` — 6 tests:
  - `generar` con secuencia 1 retorna `FAC-20260523-000001`
  - `generar` con secuencia 999999 retorna `FAC-20260523-999999`
  - `validarFormato` retorna `true` para formato correcto
  - `validarFormato` retorna `false` para null
  - `validarFormato` retorna `false` sin guiones
  - `validarFormato` retorna `false` con letras en secuencia

---

## Wave 5 — Ports (interfaces)

- [x] **5.1** Input Ports:
  - `LoginUseCase`: `login(LoginCommand): LoginResponse`
  - `ListarProductosUseCase`: `listar(): List<ProductoResponse>`
  - `RegistrarVentaUseCase`: `registrar(RegistrarVentaCommand): VentaResponse`

- [x] **5.2** Output Ports:
  - `ProductoRepositoryPort`: `findByCodigo`, `findAllActivos`, `decrementarStock`
  - `VentaRepositoryPort`: `save(Venta, List<DetalleVenta>): VentaResponse`
  - `SecuenciaFacturaRepositoryPort`: `obtenerYIncrementarSecuencia(LocalDate): int`

---

## Wave 6 — DTOs

- [x] **6.1** Commands:
  - `LoginCommand`: `username`, `password`
  - `ItemVentaCommand`: `codigoProducto`, `cantidad`
  - `RegistrarVentaCommand`: `usernameCajero` (del token, no del body), `items`, `metodoPago`, `montoPagado` (nullable), `nombreCliente` (nullable), `cedulaCliente` (nullable)

- [x] **6.2** Responses:
  - `LoginResponse`: `idToken`, `accessToken`, `expiresIn`
  - `ProductoResponse`: `id`, `codigo`, `nombre`, `descripcion`, `precio`, `stock`
  - `DetalleVentaResponse`: `codigoProducto`, `nombreProducto`, `cantidad`, `precioUnitario`, `subtotal`
  - `VentaResponse`: todos los campos incluyendo `montoPagado` (nullable) y `cambio` (nullable)

---

## Wave 7 — Servicios de aplicación

- [x] **7.1** `ProductoService` implementa `ListarProductosUseCase`:
  - Constructor recibe `ProductoRepositoryPort`
  - `listar()` llama `findAllActivos()` y convierte `Producto` → `ProductoResponse`

- [x] **7.2** `RegistrarVentaService` implementa `RegistrarVentaUseCase`:
  - Constructor recibe `ProductoRepositoryPort`, `VentaRepositoryPort`, `SecuenciaFacturaRepositoryPort`, `GeneradorNumeroFactura`, `BigDecimal tasaImpuesto`
  - Flujo completo: validar → buscar productos → validar stock → calcular → pago efectivo → factura → descontar stock → guardar
  - Validaciones: `validarMetodoPago`, `validarPagoEfectivo`, `validarDatosCliente`

- [x] **7.3** `AuthService` implementa `LoginUseCase`:
  - Constructor recibe `CognitoIdentityProviderClient` y `String userPoolClientId`
  - Invoca `InitiateAuth` con `USER_PASSWORD_AUTH`
  - Lanza `CredencialesInvalidasException` ante `NotAuthorizedException` de Cognito

---

## Wave 8 — Tests de servicios

- [x] **8.1** `ProductoServiceTest` — 2 tests:
  - `listar()` retorna lista de `ProductoResponse` cuando hay productos
  - `listar()` retorna lista vacía cuando no hay productos

- [x] **8.2** `RegistrarVentaServiceTest` — 6 tests:
  - Registra venta correctamente con `montoPagado` incluido para EFECTIVO
  - Lanza `ProductoNoEncontradoException` cuando el código no existe
  - Lanza `StockInsuficienteException` cuando el stock es insuficiente
  - Lanza `IllegalArgumentException` con método de pago inválido
  - Lanza `IllegalArgumentException` con cédula de formato incorrecto
  - Venta con cliente null no lanza error

---

## Wave 9 — Lambda Handlers

- [x] **9.1** `ApiGatewayResponse.java`:
  - `ok200(String body)`: HTTP 200 con `Content-Type: application/json`
  - `created201(String body)`: HTTP 201
  - `error(int status, String message)`: JSON `{status, error, timestamp}`

- [x] **9.2** `AuthFunction`:
  - Construye `CognitoIdentityProviderClient` y `AuthService` en el constructor
  - Lee `COGNITO_CLIENT_ID` desde variable de entorno
  - `CredencialesInvalidasException` → 401

- [x] **9.3** `ListarProductosFunction`:
  - Construye `DynamoProductoRepository` y `ProductoService` en el constructor
  - Retorna HTTP 200 con lista JSON

- [x] **9.4** `RegistrarVentaFunction`:
  - Construye todos los repositorios y `RegistrarVentaService` en el constructor
  - Extrae `username` de `event.getRequestContext().getAuthorizer().getJwt().getClaims().get("cognito:username")`
  - `ProductoNoEncontradoException` → 404
  - `StockInsuficienteException` → 422
  - `LimiteFacturasDiarioExcedidoException` → 409
  - `IllegalArgumentException` → 400

---

## Wave 10 — Repositorios DynamoDB

- [x] **10.1** `DynamoConfig.java`:
  - `buildClient()`: construye `DynamoDbClient` con `Region.of(System.getenv("AWS_REGION"))`

- [x] **10.2** `DynamoProductoRepository` implementa `ProductoRepositoryPort`:
  - Lee `TABLA_PRODUCTOS` desde variable de entorno
  - `findByCodigo`: `GetItem CODIGO#<codigo>` → `productoId` → `GetItem PROD#<id>`
  - `findAllActivos`: `Query GSI1-activo-nombre` con `activo = "true"`
  - `decrementarStock`: `UpdateItem` con `ConditionExpression: stock >= :cantidad`

- [x] **10.3** `DynamoVentaRepository` implementa `VentaRepositoryPort`:
  - Lee `TABLA_VENTAS` desde variable de entorno
  - `save`: `TransactWriteItems` con venta + índice factura (unicidad) + detalles

- [x] **10.4** `DynamoSecuenciaRepository` implementa `SecuenciaFacturaRepositoryPort`:
  - Lee `TABLA_VENTAS` desde variable de entorno
  - `obtenerYIncrementarSecuencia`: `UpdateItem ADD ultimoNumero 1` con condición `< 999999`

---

## Wave 11 — Infraestructura SAM y despliegue

- [x] **11.1** Configurar `template.yaml` con:
  - `PosUserPool`: Cognito con política de contraseñas (8 chars, mayúsculas, números)
  - `PosUserPoolClient`: `ALLOW_USER_PASSWORD_AUTH`, `ALLOW_REFRESH_TOKEN_AUTH`
  - `PosApi`: HTTP API con `CognitoAuthorizer` como default
  - `ProductosTable`: `pos-productos` con GSI `GSI1-activo-nombre`
  - `VentasTable`: `pos-ventas` sin GSI
  - `AuthFunction`: permiso `cognito-idp:InitiateAuth`, endpoint público
  - `ListarProductosFunction`: `DynamoDBReadPolicy` sobre `ProductosTable`
  - `RegistrarVentaFunction`: `DynamoDBCrudPolicy` sobre ambas tablas
  - `SnapStart: ApplyOn: PublishedVersions` en todos
  - Variables de entorno globales: `TABLA_PRODUCTOS`, `TABLA_VENTAS`, `TASA_IMPUESTO`

- [x] **11.2** Desplegar con `sam build && sam deploy --guided`:
  - Stack: `sistema-pos`
  - Región: `us-east-1`

- [x] **11.3** Crear usuario cajero de prueba:
  ```bash
  aws cognito-idp admin-create-user --user-pool-id us-east-1_ZxsTnOaDm --username cajero1 --temporary-password Temp1234!
  aws cognito-idp admin-set-user-password --user-pool-id us-east-1_ZxsTnOaDm --username cajero1 --password Pass1234! --permanent
  ```

- [x] **11.4** Insertar 10 productos de prueba en `pos-productos` via AWS CLI.

---

## Recursos desplegados

| Recurso | ID / Nombre |
|---|---|
| API URL | `https://yv2kjdpi1k.execute-api.us-east-1.amazonaws.com/prod` |
| Cognito User Pool ID | `us-east-1_ZxsTnOaDm` |
| Cognito Client ID | `12qfj6lsf16imccpjv0mq2gc44` |
| Tabla productos | `pos-productos` |
| Tabla ventas | `pos-ventas` |
| Usuario de prueba | `cajero1` / `Pass1234!` |
