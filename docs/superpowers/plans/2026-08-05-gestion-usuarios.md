# Gestion Usuarios Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar gestion administrativa de trabajadores, usuarios y roles sobre las tablas existentes `usuarios`, `roles` y `usuario_roles`.

**Architecture:** Mantener el modulo en `com.veterinaria.backend.usuario`, con controllers finos, servicios transaccionales, repositorios JPA y mapper manual. No se modifican entidades existentes ni esquema PostgreSQL; los roles se consultan en batch desde `usuario_roles` para evitar N+1.

**Tech Stack:** Java 21, Spring Boot 4.1, Maven, Spring Data JPA, Spring Security Resource Server JWT, Bean Validation, PostgreSQL, Testcontainers, MockMvc, JUnit 5, Mockito.

## Global Constraints

- Trabajar solo en la rama actual `feature/gestion-usuarios`.
- No ejecutar `git commit`, `git push`, `merge` ni cambio de rama.
- No modificar entidades existentes ni esquema PostgreSQL.
- No agregar dependencias nuevas.
- No exponer `passwordHash`, codigos de recuperacion, JWT ni credenciales.
- Proteger endpoints administrativos con `@PreAuthorize("hasRole('ADMINISTRADOR')")`.
- Usar `PUT /api/v1/usuarios/{id}` para edicion completa de datos personales editables.
- Mantener roles, estado y password en endpoints separados.
- Usar TDD: prueba fallida primero, implementacion minima despues.

---

### Task 1: DTOs, Excepciones y Mapper

**Files:**
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/CrearUsuarioRequest.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/ActualizarUsuarioRequest.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/CambiarEstadoUsuarioRequest.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/ActualizarRolesUsuarioRequest.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/UsuarioResumenResponse.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/UsuarioDetalleResponse.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/UsuarioRolesResponse.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/RolResponse.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/dto/PaginaResponse.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/exception/*.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/mapper/UsuarioMapper.java`
- Modify: `src/main/java/com/veterinaria/backend/shared/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/veterinaria/backend/usuario/mapper/UsuarioMapperTest.java`

**Interfaces:**
- Produces: request/response records, domain exceptions and `UsuarioMapper`.
- Consumes: `Usuario`, `Rol`, `NombreRol`.

- [ ] Write failing mapper tests for full name, summary/detail responses and role responses.
- [ ] Run `.\mvnw.cmd -Dtest=UsuarioMapperTest test` and verify failure because classes do not exist.
- [ ] Add DTO records with Bean Validation.
- [ ] Add domain exceptions mapped to 400, 404 and 409 in `GlobalExceptionHandler`.
- [ ] Add manual mapper.
- [ ] Re-run mapper test and verify pass.

### Task 2: Repository Queries

**Files:**
- Modify: `src/main/java/com/veterinaria/backend/usuario/repository/UsuarioRepository.java`
- Modify: `src/main/java/com/veterinaria/backend/usuario/repository/RolRepository.java`
- Modify: `src/main/java/com/veterinaria/backend/usuario/repository/UsuarioRolRepository.java`

**Interfaces:**
- Produces: query methods for case-insensitive email uniqueness, active roles, paged filtering, batch role loading and active administrator counts.

- [ ] Add failing service tests that require these repository methods.
- [ ] Implement repository signatures and JPQL queries.
- [ ] Compile with `.\mvnw.cmd -DskipTests compile`.

### Task 3: UsuarioGestionService

**Files:**
- Create: `src/main/java/com/veterinaria/backend/usuario/service/UsuarioGestionService.java`
- Test: `src/test/java/com/veterinaria/backend/usuario/service/UsuarioGestionServiceTest.java`

**Interfaces:**
- Produces:
  - `UsuarioDetalleResponse crear(CrearUsuarioRequest request)`
  - `PaginaResponse<UsuarioResumenResponse> listar(String search, Boolean activo, NombreRol rol, Pageable pageable)`
  - `UsuarioDetalleResponse obtener(Long id)`
  - `UsuarioDetalleResponse actualizar(Long id, ActualizarUsuarioRequest request)`
  - `UsuarioDetalleResponse cambiarEstado(Long id, CambiarEstadoUsuarioRequest request, Long administradorActualId)`
  - `UsuarioRolesResponse actualizarRoles(Long id, ActualizarRolesUsuarioRequest request, Long administradorActualId)`

- [ ] Write failing tests for creation, duplicate email, normalization, weak password, BCrypt hash, multiple roles, missing/inactive/duplicated roles.
- [ ] Implement minimal creation flow with transaction.
- [ ] Write failing tests for update, not found and email uniqueness excluding same user.
- [ ] Implement update flow.
- [ ] Write failing tests for state changes, self-deactivation and last active administrator.
- [ ] Implement state flow; reactivation clears `bloqueadoHasta` and resets `intentosFallidos` to 0.
- [ ] Write failing tests for role replacement and preventing removal of last administrator.
- [ ] Implement role replacement transaction.
- [ ] Write failing tests for list and detail with batch role mapping.
- [ ] Implement list/detail flows.
- [ ] Run `.\mvnw.cmd -Dtest=UsuarioGestionServiceTest test`.

### Task 4: RolConsultaService and Controllers

**Files:**
- Create: `src/main/java/com/veterinaria/backend/usuario/service/RolConsultaService.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/controller/UsuarioGestionController.java`
- Create: `src/main/java/com/veterinaria/backend/usuario/controller/RolController.java`

**Interfaces:**
- Produces endpoints:
  - `POST /api/v1/usuarios`
  - `GET /api/v1/usuarios`
  - `GET /api/v1/usuarios/{id}`
  - `PUT /api/v1/usuarios/{id}`
  - `PATCH /api/v1/usuarios/{id}/estado`
  - `PUT /api/v1/usuarios/{id}/roles`
  - `GET /api/v1/roles`

- [ ] Write failing integration tests for 401/403 and administrator access.
- [ ] Implement controllers with `@PreAuthorize("hasRole('ADMINISTRADOR')")`.
- [ ] Extract authenticated admin id from `JwtAuthenticationToken` claim `uid`.
- [ ] Run targeted integration tests.

### Task 5: Integration Coverage

**Files:**
- Create: `src/test/java/com/veterinaria/backend/usuario/UsuarioGestionIntegrationTest.java`

- [ ] Add MockMvc + Testcontainers tests for create, duplicate email, BCrypt hash, multiple roles, pagination, search, active filter, role filter, update, inactive login rejection, no physical delete, last administrator protections, active role listing and no `passwordHash`.
- [ ] Run `.\mvnw.cmd -Dtest=UsuarioGestionIntegrationTest test`.

### Task 6: README and Final Verification

**Files:**
- Modify: `README.md`

- [ ] Document endpoints, permissions, examples, rules, test commands and Postman route list.
- [ ] Run `docker version`.
- [ ] Run `.\mvnw.cmd clean test`.
- [ ] Run `.\mvnw.cmd clean verify`.
- [ ] Start app manually against `veterinaria_db` using environment variables and validate startup, Hibernate validate, Flyway behavior, `/api/v1/health`, login, admin success, unauthorized role 403 and no hardcoded credentials.
