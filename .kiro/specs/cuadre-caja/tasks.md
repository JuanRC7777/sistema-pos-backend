# Tasks — Cuadre de Caja
**Versión:** 1.0.0
**Fecha:** 2026-06-02
**Estado:** Completado y desplegado en AWS us-east-1

---

## Resumen

| Wave | Descripción | Estado |
|---|---|---|
| 1 | Dominio | ✅ Completado |
| 2 | Puertos | ✅ Completado |
| 3 | DTOs | ✅ Completado |
| 4 | Servicios | ✅ Completado |
| 5 | Infraestructura | ✅ Completado |
| 6 | SAM + despliegue | ✅ Desplegado |

---

## Wave 1 — Dominio

- [x] **1.1** Crear `Turno.java` con modelo y método `calcularCierre(efectivoContado)`
  - Calcula `efectivoEsperado = saldoInicial + totalEfectivo`
  - Calcula `diferencia = efectivoContado - efectivoEsperado`
  - Determina resultado: `CUADRADO`, `SOBRANTE` o `FALTANTE`
- [x] **1.2** Crear `TurnoYaAbiertoException` — lanzada cuando ya existe un turno `ABIERTO`
- [x] **1.3** Crear `TurnoNoEncontradoException` — lanzada cuando no hay turno activo

## Wave 2 — Puertos

- [x] **2.1** Crear `AbrirTurnoUseCase`: `abrir(AbrirTurnoCommand): TurnoResponse`
- [x] **2.2** Crear `CerrarTurnoUseCase`: `cerrar(CerrarTurnoCommand): TurnoResponse`
- [x] **2.3** Crear `ConsultarTurnoUseCase`: `consultar(): TurnoResponse`
- [x] **2.4** Crear `TurnoRepositoryPort` con `abrir`, `obtenerActivo`, `cerrar`, `acumularVenta`

## Wave 3 — DTOs

- [x] **3.1** Crear `AbrirTurnoCommand`: `usernameCajero` (del token), `saldoInicial`
- [x] **3.2** Crear `CerrarTurnoCommand`: `efectivoContado`
- [x] **3.3** Crear `TurnoResponse`: todos los campos del turno incluyendo totales por método de pago y resultado del cierre

## Wave 4 — Servicios

- [x] **4.1** Crear `TurnoService` implementando `AbrirTurnoUseCase`, `CerrarTurnoUseCase` y `ConsultarTurnoUseCase`
  - Validación: `saldoInicial` no puede ser negativo
  - Validación: `efectivoContado` no puede ser negativo
  - Previene apertura si ya hay turno activo
- [x] **4.2** Actualizar `RegistrarVentaService` para inyectar `TurnoRepositoryPort` y llamar `acumularVenta` tras cada venta exitosa

## Wave 5 — Infraestructura

- [x] **5.1** Crear `DynamoTurnoRepository`:
  - `abrir`: `PutItem PK=TURNO#ACTIVO` con `ConditionExpression: attribute_not_exists(PK)`
  - `obtenerActivo`: `GetItem PK=TURNO#ACTIVO`
  - `cerrar`: `TransactWriteItems` — guarda `TURNO#<id>` y elimina `TURNO#ACTIVO`
  - `acumularVenta`: `UpdateItem ADD totalEfectivo/totalTarjeta/totalTransferencia + cantidadVentas`
- [x] **5.2** Crear `CajaFunction` Lambda manejando los 3 endpoints según método HTTP y path

## Wave 6 — SAM y despliegue

- [x] **6.1** Agregar `CajaFunction` al `template.yaml` con `DynamoDBCrudPolicy` sobre `VentasTable` y 3 eventos HTTP
- [x] **6.2** Desplegar con `sam build && sam deploy --force-upload` y verificar funcionamiento

## Recursos desplegados

| Endpoint | URL |
|---|---|
| POST /caja/abrir | `https://yv2kjdpi1k.execute-api.us-east-1.amazonaws.com/prod/caja/abrir` |
| GET /caja/turno-actual | `https://yv2kjdpi1k.execute-api.us-east-1.amazonaws.com/prod/caja/turno-actual` |
| POST /caja/cerrar | `https://yv2kjdpi1k.execute-api.us-east-1.amazonaws.com/prod/caja/cerrar` |
