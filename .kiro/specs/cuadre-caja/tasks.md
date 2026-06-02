# Tasks — Cuadre de Caja

## Wave 1 — Dominio
- [ ] **1.1** Crear `Turno.java` con modelo y método `calcularCierre(efectivoContado)`
- [ ] **1.2** Crear `TurnoYaAbierto Exception.java`
- [ ] **1.3** Crear `TurnoNoEncontradoException.java`

## Wave 2 — Puertos
- [ ] **2.1** Crear `AbrirTurnoUseCase`
- [ ] **2.2** Crear `CerrarTurnoUseCase`
- [ ] **2.3** Crear `ConsultarTurnoUseCase`
- [ ] **2.4** Crear `TurnoRepositoryPort` con `abrir`, `obtenerActivo`, `cerrar`, `acumularVenta`

## Wave 3 — DTOs
- [ ] **3.1** Crear `AbrirTurnoCommand`
- [ ] **3.2** Crear `CerrarTurnoCommand`
- [ ] **3.3** Crear `TurnoResponse`

## Wave 4 — Servicio
- [ ] **4.1** Crear `TurnoService` implementando los 3 use cases
- [ ] **4.2** Actualizar `RegistrarVentaService` para acumular en turno activo

## Wave 5 — Infraestructura
- [ ] **5.1** Crear `DynamoTurnoRepository`
- [ ] **5.2** Crear `CajaFunction` Lambda con los 3 endpoints

## Wave 6 — SAM
- [ ] **6.1** Agregar `CajaFunction` al `template.yaml`
- [ ] **6.2** Desplegar y probar
