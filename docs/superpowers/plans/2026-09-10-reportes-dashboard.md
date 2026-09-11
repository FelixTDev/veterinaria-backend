# Plan TDD: Reportes y Dashboard

1. Crear prueba RED para validar rangos `[desde,hasta)`.
2. Implementar DTO `ReporteRango`, granularidad whitelist y validación común.
3. Crear projections y repositories específicos de reportes.
4. Implementar servicios read-only para citas, servicios, finanzas, saldos, comprobantes, vacunas y dashboard.
5. Implementar controllers con `@PreAuthorize` por ámbito.
6. Añadir integración PostgreSQL/Testcontainers para `COUNT`, `SUM`, `GROUP BY`, `DATE_TRUNC`, CTE y paginación.
7. Añadir MockMvc para 401, 403, roles autorizados, parámetros y respuestas vacías.
8. Revisar anti-N+1, doble conteo, SQL parametrizado y ausencia de datos clínicos.
9. Actualizar README y ejecutar Docker, `clean test`, `clean verify` y `git diff --check`.

No se modifican entidades, tablas, constraints, índices, migraciones ni `pom.xml`.
