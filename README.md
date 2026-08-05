# Veterinaria Backend

Backend Spring Boot para el sistema de gestion veterinaria.

## Requisitos

- Java 21
- Maven Wrapper
- PostgreSQL
- Docker Desktop o Docker Engine para Testcontainers
- Base de datos `veterinaria_db`

## Variables de entorno

Definir las siguientes variables antes de ejecutar manualmente la aplicacion:

```env
DB_URL=jdbc:postgresql://localhost:5432/veterinaria_db
DB_USERNAME=postgres
DB_PASSWORD=colocar_contrasena
JWT_SECRET=colocar_clave_de_al_menos_32_caracteres
JWT_EXPIRATION_MINUTES=60
RECOVERY_TOKEN_EXPIRATION_MINUTES=10
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
```

`JWT_SECRET` es obligatoria. La aplicacion falla al arrancar si no esta configurada.

## Autenticacion

El backend expone los siguientes endpoints del modulo `auth`:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/cambiar-password`
- `POST /api/v1/auth/recuperacion/solicitar`
- `POST /api/v1/auth/recuperacion/validar-codigo`
- `POST /api/v1/auth/recuperacion/restablecer`

La autenticacion de APIs es stateless con Bearer JWT usando Spring Security OAuth2 Resource Server y Nimbus.

- Los access tokens incluyen `purpose=access`.
- Los recovery tokens incluyen `purpose=recovery`.
- Un recovery token no autentica endpoints protegidos.
- Un access token no sirve para restablecer password.
- Los roles se convierten a authorities `ROLE_*`.

## Ejecucion

```powershell
.\mvnw.cmd spring-boot:run
```

## Pruebas

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

Las pruebas de integracion usan Testcontainers con PostgreSQL y cargan `src/test/resources/sql/script_inicial_veterinaria_postgresql.sql` como fuente de verdad del esquema.

- No usan H2.
- No dependen de `veterinaria_db`.
- No usan credenciales locales.
- El contenedor se destruye al finalizar.

## Conexion a base de datos

- Hibernate esta configurado con `ddl-auto=validate`.
- Flyway queda habilitado con `baseline-on-migrate=true` para no alterar tablas existentes que ya fueron creadas fuera del historial de migraciones.
- No se crean ni modifican tablas automaticamente.

## Recuperacion de password

El flujo consta de tres pasos:

1. Solicitar codigo: genera un codigo aleatorio de 6 digitos, guarda solo su hash BCrypt y envia correo.
2. Validar codigo: verifica hash, vigencia y estado, y devuelve un `recoveryToken` firmado de corta duracion.
3. Restablecer password: valida el `recoveryToken`, cambia la password y marca el codigo como usado dentro de una transaccion.
