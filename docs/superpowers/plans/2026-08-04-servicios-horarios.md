# Servicios y Horarios Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar catalogo, precios, horarios, indisponibilidades y disponibilidad base sobre el modelo existente.

**Architecture:** Los modulos `servicio` y `horario` tendran DTOs, mappers, services y controllers propios. Los repositories expresaran paginacion, pertenencia y solapamientos en JPQL; los services validaran roles programables, estados y transacciones.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Data JPA, Bean Validation, Spring Security method authorization, MockMvc, JUnit 5, Mockito y Testcontainers PostgreSQL.

## Global Constraints

- Mantener exclusivamente la rama `feature/servicios-horarios`.
- No hacer commit, push, merge, rebase ni cambio de rama.
- No modificar entidades, esquema PostgreSQL, migraciones, recursos SQL ni `pom.xml`.
- Solo son programables usuarios activos con rol `VETERINARIO` o `PELUQUERO`.
- Una sola jornada por trabajador y día; no jornadas partidas.
- No cancelar ni eliminar indisponibilidades porque no tienen estado.
- No considerar citas en disponibilidad.
- Reutilizar `PaginaResponse<T>` y evitar N+1, `findAll()` para filtrar y consultas en ciclos.

### Task 1: Mappers, DTOs y excepciones

**Files:** Crear DTOs, mappers y excepciones en `servicio` y `horario`; modificar `GlobalExceptionHandler.java`.

- [ ] Escribir tests de mappers para servicio, precio, horario, indisponibilidad y disponibilidad.
- [ ] Ejecutar tests y verificar fallo por clases ausentes.
- [ ] Crear DTOs con Bean Validation y sin entidades JPA.
- [ ] Crear mappers manuales sin consultas ni reglas de negocio.
- [ ] Crear excepciones de dominio solo para faltantes, roles, solapamientos y rangos inválidos.
- [ ] Integrar excepciones con 400, 404 y 409.

### Task 2: Servicios y precios

**Files:** Ampliar `ServicioRepository.java` y `PrecioServicioTamanoRepository.java`; crear `ServicioGestionService.java`; probar con `ServicioGestionServiceTest.java`.

- [ ] Escribir tests para normalización, alta, duplicado, edición, estados, filtros, detalle y precios.
- [ ] Ejecutar tests y verificar fallo por servicios ausentes.
- [ ] Añadir consultas paginadas, unicidad ignorando mayúsculas y conteo batch de precios.
- [ ] Implementar altas, ediciones, estados y reemplazo transaccional de precios; los omitidos se desactivan sin borrado físico.
- [ ] Ejecutar tests unitarios y mantenerlos verdes.

### Task 3: Horarios e indisponibilidades

**Files:** Ampliar `UsuarioRolRepository.java`, `HorarioTrabajadorRepository.java` e `IndisponibilidadTrabajadorRepository.java`; crear services; probar con tests Mockito.

- [ ] Escribir tests de roles programables, trabajador activo, rangos, descanso, pertenencia y solapamientos.
- [ ] Ejecutar tests y verificar fallo por métodos ausentes.
- [ ] Añadir consultas JPQL de existencia de roles, horarios solapados e indisponibilidades solapadas.
- [ ] Implementar `HorarioTrabajadorService` e `IndisponibilidadTrabajadorService` con validación de `uid`, transacciones y edición futura.
- [ ] Ejecutar tests unitarios y mantenerlos verdes.

### Task 4: Disponibilidad y controllers

**Files:** Crear `DisponibilidadTrabajadorService`, controllers de servicio/horario y mappers correspondientes.

- [ ] Escribir tests de disponibilidad dentro/fuera de jornada, descanso e indisponibilidad.
- [ ] Ejecutar tests y verificar fallo por service ausente.
- [ ] Implementar disponibilidad con consultas de cobertura y bloqueos, sin citas.
- [ ] Crear endpoints definitivos con `@PreAuthorize`, control de programación propia y PUT/PATCH según el modelo.
- [ ] Ejecutar pruebas unitarias y web.

### Task 5: Integración, README y verificación

**Files:** Crear `ServiciosHorariosIntegrationTest.java`; modificar `README.md`.

- [ ] Escribir pruebas Testcontainers para permisos, roles programables, servicios, precios, horarios, indisponibilidades y disponibilidad.
- [ ] Ejecutar la prueba y confirmar fallos RED antes de completar la implementación.
- [ ] Ajustar el código hasta hacer pasar todos los tests.
- [ ] Documentar endpoints, campos reales, restricciones, solapamientos, disponibilidad sin citas y pendientes.
- [ ] Ejecutar `docker version`, `./mvnw clean test` y `./mvnw clean verify`.
- [ ] Arrancar manualmente contra `veterinaria_db` usando variables de entorno, validar health, seguridad, CRUD y disponibilidad, limpiar datos y detener procesos.
- [ ] Revisar rama, diff, esquema y ausencia de credenciales expuestas.
