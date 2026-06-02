# Requirements — Cuadre de Caja

**Versión:** 1.0.0
**Fecha:** 2026-06-02
**Estado:** En implementación

---

## Introduction

Módulo de cuadre de caja para el sistema POS. Permite al cajero abrir un turno con un saldo inicial, registrar ventas durante el turno, y al finalizar declarar el efectivo físico contado para que el sistema calcule automáticamente la diferencia.

Solo existe un turno activo a la vez en el sistema (un cajero).

---

## Requirements

### RF-01: Abrir turno

**User Story:** Como cajero, quiero abrir mi turno declarando el saldo inicial en caja para que el sistema registre desde qué punto inicio mis operaciones.

#### Criterios de Aceptación

1. WHEN el cajero envía `POST /caja/abrir` con `saldoInicial`, THE sistema SHALL crear un turno con estado `ABIERTO` y registrar la fecha/hora de apertura.
2. IF ya existe un turno `ABIERTO`, THEN THE sistema SHALL retornar HTTP 409 indicando que ya hay un turno activo.
3. THE sistema SHALL identificar al cajero desde el claim `cognito:username` del token JWT.
4. IF `saldoInicial` es negativo, THEN THE sistema SHALL retornar HTTP 400.
5. THE sistema SHALL inicializar los acumuladores en cero: `totalEfectivo`, `totalTarjeta`, `totalTransferencia`, `cantidadVentas`.

---

### RF-02: Consultar turno actual

**User Story:** Como cajero, quiero consultar el estado de mi turno activo para ver cuánto he vendido.

#### Criterios de Aceptación

1. WHEN el cajero envía `GET /caja/turno-actual`, THE sistema SHALL retornar el turno activo con sus totales acumulados.
2. IF no hay turno activo, THEN THE sistema SHALL retornar HTTP 404.
3. THE sistema SHALL retornar: `saldoInicial`, `totalEfectivo`, `totalTarjeta`, `totalTransferencia`, `cantidadVentas`, `efectivoEsperado` (`saldoInicial + totalEfectivo`), `fechaApertura`, `nombreCajero`.

---

### RF-03: Cerrar turno

**User Story:** Como cajero, quiero cerrar mi turno declarando el efectivo físico contado para que el sistema me diga si cuadré o no.

#### Criterios de Aceptación

1. WHEN el cajero envía `POST /caja/cerrar` con `efectivoContado`, THE sistema SHALL calcular la diferencia y retornar el reporte de cierre.
2. IF no hay turno activo, THEN THE sistema SHALL retornar HTTP 404.
3. THE sistema SHALL calcular: `efectivoEsperado = saldoInicial + totalEfectivo`.
4. THE sistema SHALL calcular: `diferencia = efectivoContado - efectivoEsperado`.
5. THE sistema SHALL determinar el resultado: `CUADRADO` (diferencia = 0), `SOBRANTE` (diferencia > 0), `FALTANTE` (diferencia < 0).
6. THE sistema SHALL cambiar el estado del turno a `CERRADO` y registrar la fecha/hora de cierre.
7. IF `efectivoContado` es negativo, THEN THE sistema SHALL retornar HTTP 400.

---

### RF-04: Acumulación de ventas en el turno

**User Story:** Como sistema, quiero acumular automáticamente cada venta registrada en el turno activo.

#### Criterios de Aceptación

1. WHEN se registra una venta exitosamente y hay un turno `ABIERTO`, THE sistema SHALL incrementar el acumulador del método de pago correspondiente con el total de la venta.
2. THE sistema SHALL incrementar `cantidadVentas` en 1 por cada venta registrada.
3. IF no hay turno abierto, THE sistema SHALL registrar la venta normalmente sin acumular.

---

## Non-Functional Requirements

| ID | Requisito |
|---|---|
| RNF-01 | Los acumuladores del turno se actualizan de forma atómica con DynamoDB `ADD` |
| RNF-02 | El turno activo se almacena en la tabla `pos-ventas` existente sin nueva tabla |
| RNF-03 | Todos los cálculos monetarios usan `BigDecimal` con `ROUND_HALF_UP` |
