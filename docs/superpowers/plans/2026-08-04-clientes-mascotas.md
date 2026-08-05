# Gestion de Clientes y Mascotas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer gestion segura, paginada y transaccional de clientes y mascotas usando el modelo PostgreSQL existente.

**Architecture:** Dos modulos mantienen la separacion controller/service/repository/mapper/DTO/exception. Los servicios aplican reglas de negocio y los repositorios ejecutan consultas JPQL paginadas, agregadas o con propietario precargado para evitar N+1.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security method authorization, Bean Validation, MockMvc, JUnit 5, Mockito y Testcontainers PostgreSQL.

## Global Constraints

- Trabajar unicamente en `feature/clientes-mascotas`.
- No hacer commit, push, merge, rebase ni cambio de rama.
- No modificar entidades JPA ni el esquema PostgreSQL.
- No agregar dependencias.
- Reutilizar `PaginaResponse<T>`.
- No implementar DELETE ni cambio de propietario.
- Excluir direccion, tamano, microchip y observaciones por no existir en el modelo.
- No calcular ni sobrescribir `edadAproximadaAnios` automaticamente.

### Task 1: DTOs, excepciones y mappers

**Files:** Crear DTOs, excepciones y mappers en `src/main/java/com/veterinaria/backend/cliente/` y `src/main/java/com/veterinaria/backend/mascota/`; probar en `src/test/java/com/veterinaria/backend/cliente/mapper/` y `src/test/java/com/veterinaria/backend/mascota/mapper/`.

- [ ] Escribir tests de normalizacion, nombres completos, propietario resumido y exposicion condicional de edad.
- [ ] Ejecutar los tests y confirmar fallo por clases ausentes.
- [ ] Crear requests con `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Positive`, `@PastOrPresent` segun campos reales.
- [ ] Crear responses sin entidades ni relaciones JPA.
- [ ] Crear `ClienteMapper` y `MascotaMapper` manuales, sin acceso a repositorios ni calculo de edad.
- [ ] Ejecutar tests de mappers y el conjunto unitario.

### Task 2: Repositorios y servicios de clientes

**Files:** Modificar `ClienteRepository.java` y `MascotaRepository.java`; crear `ClienteGestionService.java`; probar en `ClienteGestionServiceTest.java`.

- [ ] Escribir tests unitarios para alta, trim/minusculas, documento duplicado, edicion, detalle, estado y mascotas asociadas.
- [ ] Ejecutar los tests y confirmar fallo por servicios ausentes.
- [ ] Añadir consultas `exists` por documento con y sin exclusión de id, busqueda paginada y conteo agrupado por cliente.
- [ ] Implementar altas/ediciones/estado/detalle/listados transaccionales y sin `findAll` para filtrar.
- [ ] Ejecutar tests unitarios y refactorizar solo manteniendo verde.

### Task 3: Repositorios y servicios de mascotas

**Files:** Modificar `MascotaRepository.java`; crear `MascotaGestionService.java`; probar en `MascotaGestionServiceTest.java`.

- [ ] Escribir tests para propietario inexistente/inactivo, alta, fecha futura, peso, edicion sin cambio de propietario, detalle, filtros y estados.
- [ ] Ejecutar los tests y confirmar fallo por servicios ausentes.
- [ ] Añadir consultas paginadas con propietario mediante `join fetch` o proyeccion y consulta paginada por cliente.
- [ ] Implementar validaciones y operaciones transaccionales; preservar `edadAproximadaAnios` sin recalculo.
- [ ] Ejecutar tests unitarios y refactorizar solo manteniendo verde.

### Task 4: Controllers y manejo de errores

**Files:** Crear controllers de clientes/mascotas; modificar `GlobalExceptionHandler.java`.

- [ ] Escribir pruebas MockMvc para validacion, rutas, 401 y 403.
- [ ] Ejecutar las pruebas y confirmar fallo por endpoints ausentes.
- [ ] Crear endpoints definitivos con `@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")`, PUT de edicion y PATCH de estado.
- [ ] Integrar excepciones de dominio con respuestas 400, 404 y 409.
- [ ] Ejecutar pruebas web y unitarias.

### Task 5: Integracion PostgreSQL y documentacion

**Files:** Crear `ClienteMascotaIntegrationTest.java`; modificar `README.md`; documentar rutas Postman sin crear coleccion.

- [ ] Escribir pruebas Testcontainers para roles, altas, duplicados, filtros, paginacion, propietario activo/inactivo, respuestas y persistencia tras desactivar.
- [ ] Ejecutar la prueba nueva y confirmar cada fallo esperado antes de completar la implementacion.
- [ ] Ajustar solo el codigo necesario hasta hacer pasar todas las pruebas.
- [ ] Actualizar README con campos reales, endpoints, filtros, roles, errores, anti-N+1 y pendiente de edad.
- [ ] Ejecutar `docker version`, `./mvnw clean test` y `./mvnw clean verify`.
- [ ] Arrancar manualmente contra `veterinaria_db` con variables de entorno, validar health, login, autorizacion, clientes, mascotas, filtros y detener el proceso.
- [ ] Revisar `git status` y confirmar que no hubo commits, cambios de rama ni esquema modificado.
