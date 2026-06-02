# Design — Cuadre de Caja

## DynamoDB — Tabla pos-ventas (nuevos ítems)

| PK | SK | Atributos |
|---|---|---|
| `TURNO#ACTIVO` | `METADATA` | id, nombreCajero, saldoInicial, totalEfectivo, totalTarjeta, totalTransferencia, cantidadVentas, fechaApertura, estado=ABIERTO |
| `TURNO#<id>` | `METADATA` | igual + efectivoContado, efectivoEsperado, diferencia, resultadoCierre, fechaCierre, estado=CERRADO |

El turno activo siempre tiene PK fija `TURNO#ACTIVO` — garantiza que solo exista uno.
Al cerrar, se copia a `TURNO#<id>` y se elimina `TURNO#ACTIVO`.

## Endpoints

| Método | Ruta | Lambda | Descripción |
|---|---|---|---|
| POST | `/caja/abrir` | `CajaFunction` | Abre turno con saldo inicial |
| GET | `/caja/turno-actual` | `CajaFunction` | Consulta turno activo |
| POST | `/caja/cerrar` | `CajaFunction` | Cierra turno y retorna reporte |

## Acumulación de ventas

`RegistrarVentaService` llama a `TurnoRepositoryPort.acumularVenta(metodoPago, total)` después de guardar la venta. Usa `UpdateItem ADD` atómico — si no hay turno activo simplemente no actualiza nada.

## Modelo de dominio — Turno

```java
- id: String
- nombreCajero: String
- saldoInicial: BigDecimal
- totalEfectivo: BigDecimal
- totalTarjeta: BigDecimal
- totalTransferencia: BigDecimal
- cantidadVentas: int
- fechaApertura: String
- fechaCierre: String       // nullable
- estado: String            // ABIERTO | CERRADO
- efectivoContado: BigDecimal  // nullable
- efectivoEsperado: BigDecimal // nullable
- diferencia: BigDecimal       // nullable
- resultadoCierre: String      // CUADRADO | SOBRANTE | FALTANTE | null
```
