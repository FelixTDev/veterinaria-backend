# Plan TDD: Atención de peluquería

1. Agregar primero pruebas unitarias de reglas, roles, autoría, estados, validación de archivos y compensación.
2. Implementar `storage_key`, la migración y la abstracción `ImageStorageService`.
3. Implementar el adaptador Cloudinary con variables de entorno, sin llamadas durante tests.
4. Implementar DTOs, repositorios, service y controller.
5. Modificar el cierre genérico de citas para exigir el flujo específico.
6. Agregar pruebas web e integración con PostgreSQL Testcontainers y storage mockeado.
7. Ejecutar regresiones, revisión de seguridad, `clean test`, `clean verify` y `git diff --check`.

El plan se detiene antes de Pagos hasta validar completamente esta fase.
