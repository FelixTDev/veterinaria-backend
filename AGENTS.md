# AGENTS.md

## Proyecto

Backend de un Sistema de Gestión Veterinaria.

Stack principal:

- Java 21
- Spring Boot 4.1
- Maven
- PostgreSQL
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server
- JWT con Nimbus
- Testcontainers con PostgreSQL
- MockMvc
- Arquitectura monolítica modular

## Rama de trabajo

El proyecto actualmente se desarrolla directamente sobre:

main

Antes de modificar código:

1. Verificar la rama actual.
2. Verificar que el árbol de trabajo esté limpio o identificar los cambios existentes.
3. No cambiar de rama sin autorización explícita.
4. No realizar commit, push, merge, rebase ni eliminación de ramas salvo solicitud explícita del usuario.

## Principio principal

Antes de implementar cualquier módulo:

1. Inspeccionar primero el código existente.
2. Revisar entidades, repositories, services y controllers relacionados.
3. Revisar las restricciones reales del esquema PostgreSQL.
4. Revisar enums y relaciones JPA.
5. Revisar las convenciones utilizadas por los módulos ya implementados.
6. Identificar discrepancias entre el requerimiento y el modelo real.
7. Presentar un diseño propuesto.
8. Esperar aprobación antes de realizar cambios estructurales importantes.

No inventar campos, enums, tablas ni reglas que no estén soportadas por el modelo actual.

## Skills y agentes

Antes de ejecutar una tarea, identificar qué skills y agentes globales instalados son relevantes y utilizarlos cuando aporten valor.

Priorizar especialmente los agentes:

- spring-boot-engineer
- backend-developer
- database-engineer
- test-engineer
- security-auditor
- reviewer
- architect-reviewer

Usar también otros agentes disponibles cuando la tarea lo justifique.

Priorizar skills relacionadas con:

- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Java
- Testcontainers
- Docker
- testing
- TDD
- debugging
- security best practices
- threat modeling
- API design
- code review

No usar skills o agentes de forma mecánica. Seleccionar únicamente los relevantes para la tarea.

Cuando una skill tenga un SKILL.md, leer sus instrucciones antes de aplicarla.

## Arquitectura

Mantener la arquitectura modular existente.

Flujo general:

Controller
→ Service
→ Repository
→ PostgreSQL

### Controllers

Deben:

- recibir requests;
- aplicar Bean Validation;
- aplicar autorización;
- delegar en services;
- devolver DTOs.

No deben:

- contener lógica de negocio;
- consultar repositories directamente;
- devolver entidades JPA;
- manejar manualmente excepciones ya cubiertas por GlobalExceptionHandler.

### Services

Deben:

- contener reglas de negocio;
- validar relaciones y estados;
- controlar transacciones;
- reutilizar lógica existente;
- evitar duplicación;
- manejar concurrencia cuando corresponda.

### Repositories

Deben:

- contener únicamente lógica de acceso a datos;
- realizar filtros y paginación en PostgreSQL;
- evitar consultas dentro de loops;
- evitar N+1;
- no contener reglas empresariales.

## DTOs

No devolver entidades JPA directamente.

Crear DTOs específicos para:

- requests;
- responses;
- resúmenes;
- detalles.

Reutilizar componentes existentes, por ejemplo:

PaginaResponse<T>

No duplicar DTOs equivalentes.

## Mappers

Preferir mappers manuales simples.

No agregar MapStruct salvo aprobación explícita.

Los mappers:

- no consultan repositories;
- no aplican reglas de negocio;
- no modifican entidades indirectamente.

## Persistencia

PostgreSQL es la fuente de verdad.

No modificar el esquema sin aprobación explícita.

Prohibido automáticamente:

- ALTER TABLE;
- nuevas tablas;
- nuevas columnas;
- nuevos índices;
- nuevos constraints;
- nuevas migraciones;
- nuevos enums persistidos.

Si una regla no puede implementarse con el esquema actual:

1. documentar la limitación;
2. proponer alternativas;
3. esperar aprobación.

No usar H2.

## JPA

Mantener Hibernate:

ddl-auto=validate

Evitar:

- relaciones EAGER innecesarias;
- ciclos JSON;
- fetch join de colecciones en consultas paginadas;
- N+1;
- findAll() seguido de filtros en Java.

Preferir cuando corresponda:

- JPQL;
- EntityGraph;
- proyecciones;
- consultas batch;
- JpaSpecificationExecutor.

## Transacciones

Usar:

@Transactional

en operaciones de escritura que deban ser atómicas.

Usar:

@Transactional(readOnly = true)

en consultas cuando corresponda.

No colocar @Transactional en controllers.

## Concurrencia

Cuando exista riesgo real de carrera:

- analizarlo explícitamente;
- revisar restricciones actuales de PostgreSQL;
- proponer locking o estrategia transaccional;
- no modificar esquema sin aprobación.

Si se usa bloqueo pesimista:

- justificarlo;
- mantener un orden consistente de adquisición;
- minimizar duración del lock;
- volver a validar la condición crítica después de adquirirlo.

## Seguridad

La autorización siempre debe aplicarse en backend.

No confiar en el frontend.

Mantener:

- JWT stateless;
- roles existentes;
- uid del JWT para identidad del usuario autenticado.

No recibir desde request un identificador que pueda derivarse de forma segura del JWT cuando eso introduzca riesgo de suplantación.

No exponer:

- passwordHash;
- contraseñas;
- JWT completos;
- códigos de recuperación;
- secretos;
- stack traces;
- consultas SQL internas.

## Roles actuales

Roles oficiales:

- ADMINISTRADOR
- RECEPCIONISTA
- VETERINARIO
- PELUQUERO

Trabajadores programables:

- VETERINARIO
- PELUQUERO

ADMINISTRADOR solo es programable si además posee uno de esos roles.

RECEPCIONISTA no es programable.

## Módulos implementados

Actualmente existen:

- autenticación y autorización;
- usuarios y roles;
- clientes y mascotas;
- servicios y precios;
- horarios e indisponibilidades;
- disponibilidad base;
- citas.

No romper compatibilidad con módulos anteriores al implementar uno nuevo.

## Manejo de errores

Reutilizar:

GlobalExceptionHandler

Mantener respuestas HTTP coherentes:

- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 500 Internal Server Error solo para errores inesperados

Crear excepciones de dominio específicas cuando aporten claridad.

No duplicar excepciones existentes.

## Pruebas

Aplicar TDD cuando sea razonable.

### Unitarias

- Mockito.
- Sin PostgreSQL.
- Sin H2.
- Sin levantar Spring cuando no sea necesario.

### Integración

Usar:

- MockMvc;
- PostgreSQL Testcontainers.

No tocar veterinaria_db local en pruebas automatizadas.

Las pruebas deben ser:

- independientes;
- reproducibles;
- sin contaminación entre suites.

## Fechas en tests

No usar fechas absolutas para representar futuro o pasado.

Evitar:

LocalDateTime.of(2026, ...)

Usar:

LocalDateTime.now()
    .plusDays(...)
    .withSecond(0)
    .withNano(0)

Para pasado:

LocalDateTime.now().minusDays(...)

Calcular dinámicamente día de semana cuando sea necesario.

Las pruebas no deben expirar con el calendario.

## Verificación obligatoria

Antes de considerar una tarea terminada ejecutar:

docker version

.\mvnw.cmd clean test

.\mvnw.cmd clean verify

Resultado esperado:

- BUILD SUCCESS
- 0 failures
- 0 errors

Cuando corresponda, realizar además arranque manual contra:

veterinaria_db

Validar:

- aplicación inicia;
- Hibernate validate correcto;
- Flyway sin modificaciones inesperadas;
- GET /api/v1/health devuelve 200.

Si el puerto 8080 está ocupado:

- usar otro puerto;
- no matar procesos externos sin autorización.

Detener el proceso Spring manual al finalizar.

## Git

Actualmente se trabaja directamente sobre:

main

Antes de comenzar:

git switch main
git pull origin main

No realizar automáticamente:

- commit;
- push;
- merge;
- rebase;
- cambio de rama.

Solo hacerlo cuando el usuario lo solicite explícitamente.

Antes de commit:

- git status
- git diff --stat
- git diff --check

Ejecutar clean verify antes de subir cambios.

## Dependencias

No modificar pom.xml sin:

1. explicar la necesidad;
2. verificar si puede resolverse con dependencias existentes;
3. esperar aprobación.

## Documentación

Actualizar README cuando un módulo nuevo agregue:

- endpoints;
- permisos;
- reglas relevantes;
- filtros;
- comportamiento operativo.

Mantener documentación de diseño y planes en:

docs/superpowers/

cuando la tarea lo justifique.

No incluir secretos ni credenciales.

## Entrega final de Codex

Al terminar una tarea importante, reportar:

1. archivos creados;
2. archivos modificados;
3. endpoints;
4. DTOs;
5. services;
6. repositories;
7. mappers;
8. excepciones;
9. reglas de negocio;
10. seguridad;
11. estrategia anti-N+1;
12. concurrencia;
13. pruebas unitarias;
14. pruebas de integración;
15. cantidad total de tests;
16. resultado real de clean test;
17. resultado real de clean verify;
18. resultado de arranque manual si se realizó;
19. resultado de health;
20. warnings;
21. riesgos;
22. pendientes;
23. confirmación de esquema intacto;
24. confirmación de pom.xml intacto;
25. confirmación de no exposición de credenciales;
26. estado de Git.

No afirmar que algo funciona si no fue probado.

No ocultar errores.

No maquillar resultados.