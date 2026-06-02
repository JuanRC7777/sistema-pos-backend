# Sistema POS — Backend Serverless

Backend de un sistema de punto de venta (POS) construido con arquitectura Hexagonal sobre AWS Lambda, API Gateway y DynamoDB, desplegado con AWS SAM.

---

## Arquitectura

```
Cliente (Postman / Frontend)
        │
        ▼
API Gateway HTTP API  ──→  JWT Authorizer (AWS Cognito)
        │
        ├── POST /auth/login      ──→  AuthFunction (Lambda)
        ├── GET  /productos        ──→  ListarProductosFunction (Lambda)
        ├── POST /ventas           ──→  RegistrarVentaFunction (Lambda)
        ├── POST /caja/abrir       ──→  CajaFunction (Lambda)
        ├── GET  /caja/turno-actual──→  CajaFunction (Lambda)
        └── POST /caja/cerrar      ──→  CajaFunction (Lambda)
                                              │
                              ┌───────────────┴───────────┐
                              ▼                           ▼
                      DynamoDB                      DynamoDB
                    pos-productos                 pos-ventas
```

El proyecto sigue una arquitectura **Hexagonal (Ports & Adapters)**:

- **Dominio:** modelos y reglas de negocio puras (sin dependencias de AWS)
- **Aplicación:** casos de uso e interfaces (puertos)
- **Infraestructura:** adaptadores Lambda y repositorios DynamoDB

---

## Stack tecnológico

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje principal |
| AWS Lambda + SnapStart | Funciones serverless con cold start reducido |
| API Gateway HTTP API | Exposición de endpoints REST |
| AWS Cognito | Autenticación y emisión de tokens JWT |
| DynamoDB | Base de datos NoSQL (PAY_PER_REQUEST) |
| AWS SAM | Infraestructura como código y despliegue |
| JUnit 5 + Mockito | Pruebas unitarias |

---

## Endpoints

### Autenticación
| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | No | Autenticación de cajero, retorna JWT |

### Productos
| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/productos` | JWT | Lista todos los productos activos |

### Ventas
| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/ventas` | JWT | Registra una venta y genera número de factura |

### Cuadre de Caja
| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/caja/abrir` | JWT | Abre turno con saldo inicial en caja |
| GET | `/caja/turno-actual` | JWT | Consulta totales acumulados del turno activo |
| POST | `/caja/cerrar` | JWT | Cierra turno y calcula diferencia de efectivo |

---

## Flujo de cuadre de caja

```
1. POST /caja/abrir      → cajero declara saldo inicial (ej. $50.00)
        │
        ▼
2. POST /ventas (N veces) → ventas se acumulan automáticamente en el turno
        │
        ▼
3. GET  /caja/turno-actual → consulta en tiempo real: ventas por método de pago
        │
        ▼
4. POST /caja/cerrar      → cajero declara efectivo contado
        │
        ▼
   Sistema calcula:
   efectivoEsperado = saldoInicial + totalEfectivo
   diferencia       = efectivoContado - efectivoEsperado
   resultado        = CUADRADO | SOBRANTE | FALTANTE
```

---

## Proceso SDD (Spec-Driven Development)

Este proyecto siguió un enfoque **Spec-Driven Development** usando **Kiro**: los specs fueron escritos antes de cualquier línea de código.

El orden fue:

1. **`requirements.md`** — se definieron los requisitos funcionales de cada endpoint, los datos de entrada/salida y los criterios de aceptación con formato EARS (WHEN/THE/IF/THEN).
2. **`design.md`** — con los requisitos claros, se tomaron las decisiones de arquitectura: estructura de tablas DynamoDB, contratos de endpoints y definición del `template.yaml`.
3. **`tasks.md`** — se derivó la lista de tareas de implementación en waves (dominio → puertos → DTOs → servicios → infraestructura → despliegue).
4. **Implementación** — cada clase corresponde a una tarea del spec. Si durante la implementación se descubrió algo no especificado, se actualizó el spec primero.

Los specs se encuentran en `.kiro/specs/`.

---

## URL del API desplegado

```
https://yv2kjdpi1k.execute-api.us-east-1.amazonaws.com/prod
```

---

## Instrucciones de despliegue

### Prerrequisitos

- Java 21
- Maven 3.8+
- AWS CLI configurado (`aws configure`)
- AWS SAM CLI instalado

### Pasos

```bash
# 1. Compilar y empaquetar
sam build

# 2. Desplegar (primera vez — guiado)
sam deploy --guided

# En los siguientes despliegues
sam deploy
```

Durante `sam deploy --guided` se pedirá:
- Stack name: `sistema-pos`
- AWS Region: `us-east-1`
- Confirm changes: `Y`
- Allow SAM to create IAM roles: `Y`

Al finalizar, SAM muestra los **Outputs** con la URL base del API Gateway.

---

## Variables de entorno

| Variable | Valor | Descripción |
|---|---|---|
| `TABLA_PRODUCTOS` | `pos-productos` | Nombre de la tabla de productos |
| `TABLA_VENTAS` | `pos-ventas` | Nombre de la tabla de ventas |
| `TASA_IMPUESTO` | `0.05` | Tasa de impuesto (5%) |
| `COGNITO_CLIENT_ID` | (generado por SAM) | ID del cliente Cognito |

---

## Pruebas unitarias

Las pruebas usan **Mockito** para aislar completamente DynamoDB:

```bash
mvn test
```

**26 tests — todos en verde.**

| Clase | Tests | Descripción |
|---|---|---|
| `ProductoTest` | 5 | Lógica de stock del dominio |
| `VentaTest` | 4 | Cálculo de totales e impuestos |
| `DetalleVentaTest` | 3 | Cálculo de subtotal |
| `GeneradorNumeroFacturaTest` | 6 | Formato de número de factura |
| `ProductoServiceTest` | 2 | Caso exitoso y lista vacía (DynamoDB mockeado) |
| `RegistrarVentaServiceTest` | 6 | Venta válida, producto no encontrado, stock insuficiente, método de pago inválido, cédula inválida, cliente nulo (DynamoDB mockeado) |

---

## Estructura del proyecto

```
sistema-pos/
├── .kiro/
│   ├── specs/
│   │   ├── requirements.md        ← spec principal del sistema POS
│   │   ├── design.md
│   │   ├── tasks.md
│   │   └── cuadre-caja/           ← spec del módulo de cuadre de caja
│   │       ├── requirements.md
│   │       ├── design.md
│   │       └── tasks.md
│   └── steering/
│       └── project.md             ← contexto global del proyecto
├── src/
│   ├── main/java/com/pos/
│   │   ├── domain/
│   │   │   ├── model/        (Producto, Venta, DetalleVenta, Turno)
│   │   │   ├── exception/
│   │   │   └── service/      (GeneradorNumeroFactura)
│   │   ├── application/
│   │   │   ├── port/in/      (use cases — auth, producto, venta, caja)
│   │   │   ├── port/out/     (repository ports)
│   │   │   ├── service/      (AuthService, ProductoService, RegistrarVentaService, TurnoService)
│   │   │   └── dto/
│   │   └── infrastructure/
│   │       ├── adapter/in/lambda/    (AuthFunction, ListarProductosFunction, RegistrarVentaFunction, CajaFunction)
│   │       ├── adapter/out/dynamodb/ (DynamoProductoRepository, DynamoVentaRepository, DynamoSecuenciaRepository, DynamoTurnoRepository)
│   │       └── config/
│   └── test/java/com/pos/
├── template.yaml
├── pom.xml
└── README.md
```
