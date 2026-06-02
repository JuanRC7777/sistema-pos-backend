# Requirements Document
# Sistema POS — Serverless AWS
**Versión:** 1.0.0
**Fecha:** 2026-05-25
**Estado:** Implementado y desplegado
**Stack:** Java 21 · AWS Lambda · Amazon DynamoDB · API Gateway · AWS Cognito · AWS SAM

---

## Introduction

### Propósito
Sistema Point of Sale (POS) serverless desplegado en AWS. El cajero inicia sesión con Cognito, consulta los productos disponibles escaneando o ingresando códigos, arma el carrito con cantidades, elige el método de pago y el sistema genera automáticamente un recibo con subtotal, impuesto del 5% y número de factura único. Para pagos en efectivo el sistema calcula el cambio automáticamente.

### Alcance
El sistema tiene tres módulos, uno por Lambda:

1. **AuthFunction** (`pos-auth`) — Login via AWS Cognito. Retorna JWT.
2. **ListarProductosFunction** (`pos-listar-productos`) — Retorna la lista de productos activos disponibles para el POS.
3. **RegistrarVentaFunction** (`pos-registrar-venta`) — El cajero envía los ítems del carrito y el método de pago. El sistema valida stock, calcula totales, genera número de factura y retorna el recibo.

**Fuera del alcance:** CRUD de productos (se administran directamente en DynamoDB), reembolsos, múltiples métodos de pago por venta, reportes, registro de nuevos usuarios vía API, consulta de ventas anteriores.

---

## Glossary

| Término | Descripción |
|---|---|
| Lambda | Función serverless de AWS. Permanece inactiva hasta que API Gateway la despierta al llegar una petición HTTP. |
| DynamoDB | Base de datos NoSQL de AWS. El proyecto usa dos tablas separadas: `pos-productos` y `pos-ventas`. |
| API Gateway | Enruta peticiones HTTP a funciones Lambda y valida el JWT de Cognito antes de invocarlas. |
| Cognito | Gestiona autenticación de cajeros y emite tokens JWT. |
| SAM | AWS Serverless Application Model. Define toda la infraestructura como código en `template.yaml`. |
| Cold Start | Tiempo de arranque de una Lambda que lleva inactiva. Mitigado con SnapStart en Java 21. |
| SnapStart | Configuración de Lambda que reduce el cold start de ~2s a ~200ms tomando una foto del estado inicializado. |
| PK / SK | Partition Key / Sort Key — claves de DynamoDB. |
| GSI | Global Secondary Index — índice secundario en DynamoDB para consultas por atributos no clave. |
| Handler | Clase Java que implementa `RequestHandler` de Lambda. Equivale a `@RestController` en Spring. |
| Cajero | Usuario autenticado. Su username se extrae del claim `cognito:username` del token JWT. |
| Recibo | Respuesta JSON con todos los detalles de la venta confirmada incluyendo factura, totales y cambio. |
| FAC-YYYYMMDD-NNNNNN | Formato del número de factura generado por el sistema. |
| Arquitectura Hexagonal | Patrón Ports & Adapters. El dominio es Java puro sin dependencias AWS. Los handlers y repositorios son adapters. |

---

## Requirements

### RF-01: Autenticación via AWS Cognito

**User Story:** Como cajero, quiero iniciar sesión con mis credenciales para obtener un token JWT que me permita usar el POS.

#### Criterios de Aceptación

1. WHEN el cajero envía `POST /auth/login` con `username` y `password` válidos, THE sistema SHALL invocar Cognito `InitiateAuth` con `USER_PASSWORD_AUTH` y retornar `idToken`, `accessToken` y `expiresIn`.
2. WHEN el cajero envía credenciales inválidas, THE sistema SHALL retornar HTTP 401.
3. THE sistema SHALL configurar un Cognito Authorizer en API Gateway que valide el JWT en todos los endpoints excepto `POST /auth/login`.
4. IF el token JWT está ausente, expirado o inválido, THEN API Gateway SHALL rechazar la petición con HTTP 401 sin invocar la Lambda.
5. THE sistema SHALL extraer el `username` del cajero desde el claim `cognito:username` del token — no del body del request.
6. THE Cognito User Pool SHALL requerir contraseñas con mínimo 8 caracteres, mayúsculas y números.

---

### RF-02: Listar Productos

**User Story:** Como cajero autenticado, quiero ver la lista de productos disponibles para poder ingresar sus códigos al registrar una venta.

#### Criterios de Aceptación

1. WHEN el cajero envía `GET /productos` con JWT válido, THE sistema SHALL retornar solo los productos con `activo = "true"` desde la tabla `pos-productos`.
2. THE sistema SHALL retornar para cada producto: `id`, `codigo`, `nombre`, `descripcion`, `precio` y `stock`.
3. THE sistema SHALL consultar productos usando el GSI `GSI1-activo-nombre` ordenados alfabéticamente por nombre.
4. IF no hay productos activos, THE sistema SHALL retornar HTTP 200 con lista vacía `[]`.
5. THE sistema SHALL requerir token JWT válido para acceder a este endpoint.

---

### RF-03: Registrar Venta POS

**User Story:** Como cajero autenticado, quiero registrar una venta ingresando códigos de producto, cantidades y método de pago, para que el sistema genere el recibo automáticamente.

#### Criterios de Aceptación

1. WHEN el cajero envía `POST /ventas` con `items[]` y `metodoPago`, THE sistema SHALL retornar el recibo con HTTP 201.
2. THE sistema SHALL identificar al cajero desde el claim `cognito:username` del token JWT.
3. THE sistema SHALL buscar cada producto por `codigoProducto` en la tabla `pos-productos`.
4. IF un `codigoProducto` no existe, THEN THE sistema SHALL retornar HTTP 404 con mensaje descriptivo.
5. IF el stock es insuficiente, THEN THE sistema SHALL retornar HTTP 422 con el nombre del producto y stock disponible.
6. THE sistema SHALL validar que la cantidad de cada ítem sea mayor a cero. Si no, retornar HTTP 400.
7. THE sistema SHALL validar que `metodoPago` sea uno de: `EFECTIVO`, `TARJETA`, `TRANSFERENCIA`. Si no, retornar HTTP 400.
8. WHEN la venta es confirmada, THE sistema SHALL descontar el stock usando DynamoDB `UpdateItem` con `ConditionExpression: stock >= :cantidad`.
9. WHEN la venta es confirmada, THE sistema SHALL generar un número de factura único con formato `FAC-YYYYMMDD-NNNNNN`.
10. THE sistema SHALL registrar el precio unitario del producto al momento de la venta, no el precio actual futuro.
11. THE sistema SHALL aceptar los campos opcionales `nombreCliente` y `cedulaCliente`.
12. IF `cedulaCliente` es provista, THEN THE sistema SHALL validar que tenga exactamente 10 dígitos numéricos. Si no, retornar HTTP 400.
13. IF `nombreCliente` es provisto, THEN THE sistema SHALL validar que tenga máximo 100 caracteres. Si no, retornar HTTP 400.

---

### RF-04: Pago en Efectivo y Cálculo de Cambio

**User Story:** Como cajero, quiero ingresar el monto que me da el cliente en efectivo y que el sistema calcule automáticamente el cambio a devolver.

#### Criterios de Aceptación

1. IF `metodoPago` es `EFECTIVO`, THEN el campo `montoPagado` es obligatorio. Si está ausente, retornar HTTP 400.
2. IF `montoPagado` es menor o igual a cero, THEN THE sistema SHALL retornar HTTP 400.
3. IF `montoPagado` es menor al total de la venta, THEN THE sistema SHALL retornar HTTP 400 indicando el total a pagar y el monto recibido.
4. WHEN el pago en efectivo es válido, THE sistema SHALL calcular el cambio como `montoPagado - total`, redondeado a 2 decimales.
5. THE sistema SHALL incluir `montoPagado` y `cambio` en el recibo cuando `metodoPago` es `EFECTIVO`.
6. IF `metodoPago` es `TARJETA` o `TRANSFERENCIA`, THEN `montoPagado` no es requerido y `cambio` no aparece en el recibo.

---

### RF-05: Cálculo de Totales

**User Story:** Como cajero, quiero que el sistema calcule automáticamente subtotales, impuesto y total.

#### Criterios de Aceptación

1. THE sistema SHALL calcular el subtotal por línea como `precio_unitario × cantidad`, redondeado a 2 decimales con `ROUND_HALF_UP`.
2. THE sistema SHALL calcular el subtotal general como la suma de todos los subtotales de línea.
3. THE sistema SHALL calcular el impuesto como `subtotal_general × 0.05`, redondeado a 2 decimales con `ROUND_HALF_UP`.
4. THE sistema SHALL calcular el total final como `subtotal_general + impuesto`, redondeado a 2 decimales con `ROUND_HALF_UP`.
5. THE sistema SHALL usar `BigDecimal` para todos los cálculos monetarios.
6. THE sistema SHALL leer la tasa de impuesto desde la variable de entorno `TASA_IMPUESTO` (valor configurado: `0.05`).

---

### RF-06: Número de Factura Único

**User Story:** Como cajero, quiero que cada recibo tenga un número de factura único y con formato estándar.

#### Criterios de Aceptación

1. THE sistema SHALL generar el número de factura con formato `FAC-YYYYMMDD-NNNNNN`.
2. THE sistema SHALL garantizar unicidad del número de factura mediante DynamoDB `TransactWriteItems` con `ConditionExpression: attribute_not_exists(PK)`.
3. THE sistema SHALL usar un contador atómico (`ADD ultimoNumero 1`) en la tabla `pos-ventas` con clave `SEQ#<fecha>`.
4. IF se alcanzan 999,999 facturas en un día, THE sistema SHALL retornar HTTP 409.

---

## Non-Functional Requirements

### RNF-01: Seguridad
- Toda comunicación usa HTTPS (API Gateway lo garantiza automáticamente).
- Cognito gestiona contraseñas y tokens. No se almacenan contraseñas en DynamoDB.
- Roles IAM con permisos mínimos por Lambda: `pos-auth` solo puede hacer `InitiateAuth`; `pos-listar-productos` solo puede leer `pos-productos`; `pos-registrar-venta` puede leer y escribir en ambas tablas.

### RNF-02: Rendimiento
- Cold start mitigado con Lambda SnapStart (`ApplyOn: PublishedVersions`).
- DynamoDB en modo `PAY_PER_REQUEST` (On-Demand) — sin capacidad provisionada.
- Timeout de Lambda: 30 segundos. Memoria: 512 MB.

### RNF-03: Arquitectura Hexagonal (Ports & Adapters)
- El dominio (`domain/model`) es Java puro sin dependencias AWS ni frameworks.
- Los casos de uso (`application/service`) dependen solo de interfaces (ports).
- Los Lambda handlers son adapters primarios — equivalen a `@RestController` en Spring.
- Los repositorios DynamoDB son adapters secundarios — equivalen a `@Repository` en Spring.
- No hay `@Autowired`, `@Bean`, `@Entity` ni ninguna anotación de framework. La DI es manual en el constructor de cada handler.

### RNF-04: Principios SOLID
- **SRP**: Cada Lambda tiene una única responsabilidad.
- **OCP**: Agregar un nuevo adapter no modifica el dominio ni los servicios.
- **LSP**: Cualquier implementación de `ProductoRepositoryPort` puede sustituirse por un mock en tests.
- **ISP**: Cada handler inyecta solo el use case que necesita.
- **DIP**: Los servicios dependen de interfaces, no de implementaciones concretas.

### RNF-05: Testing
- Tests unitarios del dominio sin dependencias AWS (JUnit 5 + AssertJ).
- Tests de servicios con mocks (Mockito) — sin contexto Spring.
- 30 tests en total, todos pasando.

---

## Business Rules

| ID | Regla | Capa |
|---|---|---|
| RN-01 | No se puede registrar una venta con cantidad igual a cero. | Application Service |
| RN-02 | No se puede vender un producto con stock insuficiente. | Domain (`Producto.descontarStock`) |
| RN-03 | El stock se descuenta con DynamoDB conditional write. | Infrastructure |
| RN-04 | El precio de venta es el precio del producto al momento de confirmar. | Application Service |
| RN-05 | Solo usuarios autenticados acceden al sistema (excepto `/auth/login`). | Infrastructure (API Gateway) |
| RN-06 | El subtotal por línea es `precio_unitario × cantidad`, ROUND_HALF_UP a 2 decimales. | Domain |
| RN-07 | El impuesto es `subtotal × 0.05`, ROUND_HALF_UP a 2 decimales. | Domain |
| RN-08 | El total es `subtotal + impuesto`, ROUND_HALF_UP a 2 decimales. | Domain |
| RN-09 | El método de pago debe ser EFECTIVO, TARJETA o TRANSFERENCIA. | Application Service |
| RN-10 | Si el método de pago es EFECTIVO, `montoPagado` es obligatorio. | Application Service |
| RN-11 | El monto pagado en efectivo no puede ser menor al total. | Application Service |
| RN-12 | El cambio es `montoPagado - total`, ROUND_HALF_UP a 2 decimales. | Domain |
| RN-13 | El número de factura es único. Garantizado por DynamoDB TransactWriteItems. | Infrastructure |
| RN-14 | El número de factura sigue el formato FAC-YYYYMMDD-NNNNNN. | Domain Service |
| RN-15 | La secuencia de factura se reinicia a 000001 cada día. | Application Service |
| RN-16 | El cajero se identifica desde el claim `cognito:username` del token JWT. | Infrastructure (Handler) |
| RN-17 | La tasa de impuesto es 5%, definida en variable de entorno `TASA_IMPUESTO`. | Infrastructure |
| RN-18 | El dominio no depende de ningún framework ni SDK externo. | Domain |
| RN-19 | `nombreCliente` y `cedulaCliente` son siempre opcionales. | Application Service |
| RN-20 | Si se provee `cedulaCliente`, debe tener exactamente 10 dígitos numéricos. | Application Service |
