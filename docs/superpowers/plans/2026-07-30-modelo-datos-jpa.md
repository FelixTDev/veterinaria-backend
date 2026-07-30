# Modelo De Datos JPA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mapear el esquema PostgreSQL existente del sistema veterinario a entidades JPA, enums y repositorios Spring Data JPA con pruebas aisladas en PostgreSQL Testcontainers.

**Architecture:** El mapeo se organiza por modulos de negocio bajo `com.veterinaria.backend`, manteniendo relaciones JPA mayormente unidireccionales y auditando timestamps sin competir con triggers de PostgreSQL. Las pruebas de persistencia usan un contenedor PostgreSQL temporal cargado con el script SQL exacto como fuente de verdad y Hibernate en modo `validate`.

**Tech Stack:** Spring Boot, Spring Data JPA, PostgreSQL, Testcontainers, JUnit 5, Maven, Java 21.

## Global Constraints

- Mantener `spring.jpa.hibernate.ddl-auto=validate`.
- `DB_PASSWORD` obligatoria y sin valor por defecto.
- Las pruebas no deben tocar `veterinaria_db` local.
- No usar H2.
- El script SQL proporcionado debe ser la fuente de verdad.
- No modificar ni duplicar el esquema en migraciones.
- No implementar controladores CRUD, servicios CRUD ni logica de negocio.
- No hacer commit, push ni cambiar de rama.
- Mantener relaciones unidireccionales por defecto.
- Evitar `CascadeType.ALL` salvo justificacion concreta.
- No usar Lombok `@Data` en entidades.
- Usar `BigDecimal`, `LocalDate`, `LocalTime` y `LocalDateTime` segun corresponda.
- Respetar `created_at` y `updated_at` sin competir con triggers de PostgreSQL.

---

### Task 1: Configuracion Base De Runtime Y Pruebas

**Files:**
- Modify: `C:\Proyectos\Veterinaria\veterinaria-backend\pom.xml`
- Modify: `C:\Proyectos\Veterinaria\veterinaria-backend\src\main\resources\application.properties`
- Modify: `C:\Proyectos\Veterinaria\veterinaria-backend\.env.example`
- Create: `C:\Proyectos\Veterinaria\veterinaria-backend\src\test\resources\sql\script_inicial_veterinaria_postgresql.sql`
- Create: `C:\Proyectos\Veterinaria\veterinaria-backend\src\test\resources\application-test.properties`

**Interfaces:**
- Consumes: Script SQL fuente de verdad.
- Produces: Configuracion reproducible para runtime local y pruebas con Testcontainers.

### Task 2: Pruebas De Persistencia En Rojo

**Files:**
- Create: `C:\Proyectos\Veterinaria\veterinaria-backend\src\test\java\com\veterinaria\backend\support\PostgreSqlContainerConfiguration.java`
- Create: `C:\Proyectos\Veterinaria\veterinaria-backend\src\test\java\com\veterinaria\backend\persistence\JpaSchemaValidationTest.java`
- Create: `C:\Proyectos\Veterinaria\veterinaria-backend\src\test\java\com\veterinaria\backend\persistence\PersistenceMappingIntegrationTest.java`

**Interfaces:**
- Consumes: Repositorios y entidades a crear.
- Produces: Cobertura de carga de contexto JPA, consulta de roles, persistencia de cliente/mascota, usuario-rol y servicios/precios.

### Task 3: Modelo Compartido Y Bloques De Seguridad/Clientes/Horarios/Servicios

**Files:**
- Create/Modify: paquetes `shared`, `usuario`, `auth`, `cliente`, `mascota`, `horario`, `servicio`

**Interfaces:**
- Consumes: Script SQL y tests de Task 2.
- Produces: Entidades, enums y repositorios base para seguridad, clientes, mascotas, horarios y servicios.

### Task 4: Modelo De Citas, Atencion Medica, Peluqueria, Pagos Y Comprobantes

**Files:**
- Create/Modify: paquetes `cita`, `atencionmedica`, `vacuna`, `peluqueria`, `pago`, `comprobante`

**Interfaces:**
- Consumes: Entidades base de Task 3.
- Produces: Resto del grafo JPA y repositorios necesarios.

### Task 5: Verificacion Final

**Files:**
- Modify: tests/documentacion minima necesaria

**Interfaces:**
- Consumes: Proyecto completo.
- Produces: `.\mvnw.cmd clean verify` y arranque manual de la aplicacion contra PostgreSQL local con validacion Hibernate y endpoint de salud.
