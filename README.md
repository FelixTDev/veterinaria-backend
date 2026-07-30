# Veterinaria Backend

Backend Spring Boot para el sistema de gestion veterinaria.

## Requisitos

- Java 21
- Maven Wrapper
- PostgreSQL
- Base de datos `veterinaria_db`

## Variables de entorno

Definir las siguientes variables antes de ejecutar el proyecto o las pruebas de integracion:

```env
DB_URL=jdbc:postgresql://localhost:5432/veterinaria_db
DB_USERNAME=postgres
DB_PASSWORD=colocar_contrasena
```

## Ejecucion

```powershell
.\mvnw.cmd spring-boot:run
```

## Pruebas

```powershell
.\mvnw.cmd test
```

La prueba `DatabaseConnectionIntegrationTest` valida la apertura de una conexion real cuando `DB_USERNAME` y `DB_PASSWORD` estan definidas.

## Conexion a base de datos

- Hibernate esta configurado con `ddl-auto=none`.
- Flyway queda habilitado con `baseline-on-migrate=true` para no alterar tablas existentes que ya fueron creadas fuera del historial de migraciones.
- No se crean ni modifican tablas automaticamente.
