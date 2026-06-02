# Design Document: Sistema POS — Serverless AWS
**Versión:** 1.0.0
**Fecha:** 2026-05-25
**Estado:** Implementado y desplegado en AWS
**Stack:** Java 21 · AWS Lambda · Amazon DynamoDB · API Gateway · AWS Cognito · AWS SAM

---

## Overview

Sistema Point of Sale serverless en AWS con 3 funciones Lambda, 2 tablas DynamoDB, 1 API Gateway y 1 Cognito User Pool. El cajero inicia sesión via Cognito, consulta productos activos, arma el carrito y registra la venta. La arquitectura sigue Ports & Adapters (hexagonal) con dominio Java puro sin dependencias AWS.

### Recursos AWS desplegados

| Recurso | Nombre | Descripción |
|---|---|---|
| API Gateway | `PosApi` | HTTP API con Cognito Authorizer |
| Cognito User Pool | `pos-user-pool` | Gestiona cajeros y emite JWT |
| DynamoDB | `pos-productos` | Tabla de productos con GSI |
| DynamoDB | `pos-ventas` | Tabla de ventas, detalles y secuencias |
| Lambda | `pos-auth` | Login |
| Lambda | `pos-listar-productos` | Listar productos activos |
| Lambda | `pos-registrar-venta` | Registrar venta y generar recibo |

---

## Architecture

### Flujo General

```
Cliente (Postman / App)
    │
    ▼
API Gateway HTTP API (PosApi)
    │
    ├── POST /auth/login ──── (sin JWT) ──────────► pos-auth
    │                                                   │
    │                                               Cognito InitiateAuth
    │                                                   │
    │                                            ◄── idToken + accessToken
    │
    ├── GET /productos ────── (JWT válido) ────────► pos-listar-productos
    │                                                   │
    │                                               DynamoDB GSI1 (pos-productos)
    │                                                   │
    │                                            ◄── Lista de productos activos
    │
    └── POST /ventas ─────── (JWT válido) ────────► pos-registrar-venta
                                                        │
                                                    1. Buscar productos (pos-productos)
                                                    2. Validar stock
                                                    3. Calcular totales + impuesto
                                                    4. Generar número factura (pos-ventas SEQ#)
                                                    5. Descontar stock (pos-productos)
                                                    6. Guardar venta (pos-ventas TransactWrite)
                                                        │
                                                 ◄── Recibo HTTP 201
```

### Capas de la Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────┐
│  INFRASTRUCTURE — Adapters                              │
│                                                         │
│  Primarios (IN):          Secundarios (OUT):            │
│  - AuthFunction           - DynamoProductoRepository    │
│  - ListarProductosFunction- DynamoVentaRepository       │
│  - RegistrarVentaFunction - DynamoSecuenciaRepository   │
│  - ApiGatewayResponse     - DynamoConfig                │
├─────────────────────────────────────────────────────────┤
│  APPLICATION — Use Cases                                │
│                                                         │
│  Ports IN:                Ports OUT:                    │
│  - LoginUseCase           - ProductoRepositoryPort      │
│  - ListarProductosUseCase - VentaRepositoryPort         │
│  - RegistrarVentaUseCase  - SecuenciaFacturaRepositoryPort│
│                                                         │
│  Services:                DTOs:                         │
│  - AuthService            - LoginCommand/Response       │
│  - ProductoService        - RegistrarVentaCommand       │
│  - RegistrarVentaService  - ItemVentaCommand            │
│                           - ProductoResponse            │
│                           - VentaResponse               │
│                           - DetalleVentaResponse        │
├─────────────────────────────────────────────────────────┤
│  DOMAIN — Java Puro (sin dependencias AWS)              │
│                                                         │
│  Models:                  Exceptions:                   │
│  - Producto               - StockInsuficienteException  │
│  - Venta                  - ProductoNoEncontradoException│
│  - DetalleVenta           - VentaNoEncontradaException  │
│                           - CredencialesInvalidasException│
│  Services:                - LimiteFacturasDiarioExcedido │
│  - GeneradorNumeroFactura                               │
└─────────────────────────────────────────────────────────┘
```

---

## API Endpoints

| Método | Endpoint | Lambda | Auth | Descripción |
|---|---|---|---|---|
| POST | `/auth/login` | `pos-auth` | No | Login, retorna JWT |
| GET | `/productos` | `pos-listar-productos` | Cognito JWT | Lista productos activos |
| POST | `/ventas` | `pos-registrar-venta` | Cognito JWT | Registra venta, retorna recibo |

---

## Domain Models

### Producto
```java
// domain/model/Producto.java
- id: String
- codigo: String
- nombre: String
- descripcion: String
- precio: BigDecimal
- stock: int
- activo: boolean

+ tieneStockSuficiente(int cantidad): boolean  // stock >= cantidad
+ descontarStock(int cantidad): void            // lanza StockInsuficienteException si insuficiente
```

### DetalleVenta
```java
// domain/model/DetalleVenta.java
- ventaId: String
- productoId: String
- codigoProducto: String
- nombreProducto: String
- cantidad: int
- precioUnitario: BigDecimal
- subtotal: BigDecimal

+ calcularSubtotal(): BigDecimal  // precioUnitario × cantidad, ROUND_HALF_UP
```

### Venta
```java
// domain/model/Venta.java
- id: String
- numeroFactura: String
- nombreCajero: String
- nombreCliente: String     // nullable
- cedulaCliente: String     // nullable
- detalles: List<DetalleVenta>
- metodoPago: String        // EFECTIVO | TARJETA | TRANSFERENCIA
- subtotal: BigDecimal
- tasaImpuesto: BigDecimal
- impuesto: BigDecimal
- total: BigDecimal
- montoPagado: BigDecimal   // nullable — solo EFECTIVO
- cambio: BigDecimal        // nullable — solo EFECTIVO
- fecha: String             // ISO-8601 UTC

+ calcularTotales(): void   // suma subtotales → impuesto (×0.05) → total
+ calcularCambio(): void    // cambio = montoPagado - total
```

### GeneradorNumeroFactura
```java
// domain/service/GeneradorNumeroFactura.java
+ generar(LocalDate fecha, int secuencia): String  // "FAC-20260525-000001"
+ validarFormato(String numeroFactura): boolean    // regex ^FAC-\d{8}-\d{6}$
```

---

## Application Services

### RegistrarVentaService — flujo completo
```
1. validarMetodoPago(cmd.metodoPago)
2. validarDatosCliente(cmd.nombreCliente, cmd.cedulaCliente)
3. Por cada ítem:
   a. productoRepo.findByCodigo(codigo) → ProductoNoEncontradoException si no existe
   b. producto.tieneStockSuficiente(cantidad) → StockInsuficienteException si no
   c. Construir DetalleVenta con precio al momento de la venta
   d. detalle.calcularSubtotal()
4. Construir Venta con tasaImpuesto = 0.05
5. venta.calcularTotales()
6. Si EFECTIVO: validarPagoEfectivo(montoPagado, total) → venta.calcularCambio()
7. secuenciaRepo.obtenerYIncrementarSecuencia(hoy) → número único
8. venta.setNumeroFactura(generador.generar(hoy, secuencia))
9. Por cada ítem: productoRepo.decrementarStock(productoId, cantidad)
10. ventaRepo.save(venta, detalles) → VentaResponse
```

---

## Infrastructure

### DynamoDB — Tabla pos-productos

| Entidad | PK | SK | Atributos |
|---|---|---|---|
| Producto | `PROD#<id>` | `METADATA` | `codigo`, `nombre`, `descripcion`, `precio`, `stock`, `activo`, `productoId` |
| Índice código | `CODIGO#<codigo>` | `METADATA` | `productoId` |

**GSI:** `GSI1-activo-nombre`
- PK: `activo` (String: `"true"` / `"false"`)
- SK: `nombre` (String)
- Uso: `GET /productos` — query con `activo = "true"` retorna productos ordenados por nombre

**Operación descontar stock:**
```
UpdateItem
  Key: PK="PROD#<id>", SK="METADATA"
  UpdateExpression: SET stock = stock - :cantidad
  ConditionExpression: stock >= :cantidad
  → ConditionalCheckFailedException si stock insuficiente
```

---

### DynamoDB — Tabla pos-ventas

| Entidad | PK | SK | Atributos |
|---|---|---|---|
| Venta | `VENTA#<id>` | `METADATA` | `id`, `numeroFactura`, `nombreCajero`, `metodoPago`, `subtotal`, `tasaImpuesto`, `impuesto`, `total`, `fecha`, `nombreCliente?`, `cedulaCliente?`, `montoPagado?`, `cambio?` |
| Detalle | `VENTA#<id>` | `DETALLE#<productoId>` | `codigoProducto`, `nombreProducto`, `cantidad`, `precioUnitario`, `subtotal` |
| Índice factura | `FACTURA#<numeroFactura>` | `METADATA` | `ventaId` |
| Secuencia | `SEQ#<fecha>` | `METADATA` | `ultimoNumero` |

**Operación guardar venta (TransactWriteItems):**
```
TransactWriteItems:
  1. PutItem VENTA#<id> METADATA        → ConditionExpression: attribute_not_exists(PK)
  2. PutItem FACTURA#<numero> METADATA  → ConditionExpression: attribute_not_exists(PK) (unicidad)
  3. PutItem VENTA#<id> DETALLE#<prod>  → por cada ítem del carrito
```

**Operación secuencia de factura:**
```
UpdateItem
  Key: PK="SEQ#20260525", SK="METADATA"
  UpdateExpression: ADD ultimoNumero :uno
  ConditionExpression: attribute_not_exists(ultimoNumero) OR ultimoNumero < 999999
  ReturnValues: ALL_NEW
  → ConditionalCheckFailedException si límite excedido
```

---

### Lambda Handlers

Todos implementan `RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>`.

La inyección de dependencias es **manual en el constructor** — no hay Spring, no hay `@Autowired`.

```java
// Ejemplo: RegistrarVentaFunction constructor
public RegistrarVentaFunction() {
    DynamoDbClient client = DynamoConfig.buildClient();
    BigDecimal tasaImpuesto = new BigDecimal(System.getenv("TASA_IMPUESTO"));

    this.registrarVentaUseCase = new RegistrarVentaService(
        new DynamoProductoRepository(client),
        new DynamoVentaRepository(client),
        new DynamoSecuenciaRepository(client),
        new GeneradorNumeroFactura(),
        tasaImpuesto
    );
}
```

**Extracción del username desde el token JWT:**
```java
String username = event.getRequestContext()
    .getAuthorizer()
    .getJwt()
    .getClaims()
    .get("cognito:username");
```

---

### Variables de Entorno Lambda

| Variable | Valor | Usado en |
|---|---|---|
| `TABLA_PRODUCTOS` | `pos-productos` | `DynamoProductoRepository` |
| `TABLA_VENTAS` | `pos-ventas` | `DynamoVentaRepository`, `DynamoSecuenciaRepository` |
| `TASA_IMPUESTO` | `0.05` | `RegistrarVentaFunction` constructor |
| `COGNITO_CLIENT_ID` | `<id del cliente>` | `AuthFunction` constructor |
| `AWS_REGION` | `us-east-1` | `DynamoConfig.buildClient()` |

---

## Request / Response Examples

### POST /auth/login
```json
// Request
{ "username": "cajero1", "password": "Pass1234!" }

// Response 200
{
  "idToken": "eyJraWQi...",
  "accessToken": "eyJraWQi...",
  "expiresIn": 3600
}
```

### GET /productos
```json
// Response 200
[
  { "id": "1", "codigo": "BEB-001", "nombre": "Bebida Cola 500ml", "descripcion": "Bebida gaseosa cola", "precio": 1.5, "stock": 100 },
  { "id": "2", "codigo": "BEB-002", "nombre": "Agua Mineral 500ml", "descripcion": "Agua mineral sin gas", "precio": 1.0, "stock": 150 }
]
```

### POST /ventas — EFECTIVO
```json
// Request
{
  "items": [
    { "codigoProducto": "BEB-001", "cantidad": 2 },
    { "codigoProducto": "SNK-001", "cantidad": 3 }
  ],
  "metodoPago": "EFECTIVO",
  "montoPagado": 10.00,
  "nombreCliente": "Juan Pérez",
  "cedulaCliente": "1234567890"
}

// Response 201
{
  "id": "uuid-generado",
  "numeroFactura": "FAC-20260525-000001",
  "fecha": "2026-05-25T02:48:00Z",
  "nombreCajero": "cajero1",
  "nombreCliente": "Juan Pérez",
  "cedulaCliente": "1234567890",
  "detalles": [
    { "codigoProducto": "BEB-001", "nombreProducto": "Bebida Cola 500ml", "cantidad": 2, "precioUnitario": 1.50, "subtotal": 3.00 },
    { "codigoProducto": "SNK-001", "nombreProducto": "Papas Fritas 50g", "cantidad": 3, "precioUnitario": 0.75, "subtotal": 2.25 }
  ],
  "metodoPago": "EFECTIVO",
  "subtotal": 5.25,
  "tasaImpuesto": 0.05,
  "impuesto": 0.26,
  "total": 5.51,
  "montoPagado": 10.00,
  "cambio": 4.49
}
```

---

## Error Handling

| Excepción | HTTP | Cuándo ocurre |
|---|---|---|
| `CredencialesInvalidasException` | 401 | Usuario o contraseña incorrectos en Cognito |
| JWT ausente/expirado/inválido | 401 | API Gateway rechaza antes de invocar Lambda |
| `ProductoNoEncontradoException` | 404 | Código de producto no existe en `pos-productos` |
| `StockInsuficienteException` | 422 | Stock insuficiente para la cantidad solicitada |
| `LimiteFacturasDiarioExcedidoException` | 409 | Se alcanzaron 999,999 facturas en el día |
| `IllegalArgumentException` | 400 | Validaciones: cantidad cero, método de pago inválido, monto insuficiente, cédula inválida, `montoPagado` ausente en EFECTIVO |
| Error inesperado | 500 | Error no controlado en la Lambda |

```json
// Formato estándar de error
{
  "status": 400,
  "error": "El monto pagado (1.00) es insuficiente. Total a pagar: 5.51",
  "timestamp": "2026-05-25T02:48:55Z"
}
```

---

## IAM Permissions

| Lambda | Tabla productos | Tabla ventas | Cognito |
|---|---|---|---|
| `pos-auth` | — | — | `InitiateAuth` |
| `pos-listar-productos` | Read | — | — |
| `pos-registrar-venta` | Read + Write | Read + Write | — |

---

## Testing

| Tipo | Clases | Tests |
|---|---|---|
| Unit — dominio | `ProductoTest`, `DetalleVentaTest`, `VentaTest`, `GeneradorNumeroFacturaTest` | 18 |
| Unit — servicios | `ProductoServiceTest`, `RegistrarVentaServiceTest`, `ObtenerVentaServiceTest` | 12 |
| **Total** | **7 clases** | **30 tests** |

Herramientas: JUnit 5 + AssertJ + Mockito. Sin contexto Spring — mocks manuales con `Mockito.mock(...)`.
