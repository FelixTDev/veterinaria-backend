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

## Gestion de usuarios y trabajadores

Los endpoints administrativos del modulo de usuarios requieren Bearer JWT con `ROLE_ADMINISTRADOR`.

Ruta base de trabajadores:

- `POST /api/v1/usuarios`: registra un trabajador activo con una o varias asignaciones en `usuario_roles`.
- `GET /api/v1/usuarios`: lista trabajadores con paginacion y filtros `page`, `size`, `sort`, `search`, `activo` y `rol`.
- `GET /api/v1/usuarios/{id}`: obtiene el detalle de un trabajador.
- `PUT /api/v1/usuarios/{id}`: actualiza datos personales editables. No modifica password, roles ni estado.
- `PATCH /api/v1/usuarios/{id}/estado`: activa o desactiva una cuenta sin eliminarla fisicamente.
- `PUT /api/v1/usuarios/{id}/roles`: reemplaza los roles asignados.

Ruta de roles disponibles:

- `GET /api/v1/roles`: lista los roles activos.

Ejemplo de registro:

```json
{
  "primerNombre": "Ana",
  "segundoNombre": null,
  "primerApellido": "Torres",
  "segundoApellido": "Lopez",
  "correo": "ana.torres@veterinaria.com",
  "telefono": "999888777",
  "passwordInicial": "Password123*",
  "roles": ["RECEPCIONISTA", "PELUQUERO"]
}
```

Ejemplo de edicion de datos personales:

```json
{
  "primerNombre": "Ana Maria",
  "segundoNombre": null,
  "primerApellido": "Torres",
  "segundoApellido": "Lopez",
  "correo": "ana.torres@veterinaria.com",
  "telefono": "999111222"
}
```

Ejemplo de cambio de estado:

```json
{
  "activo": false
}
```

Ejemplo de reemplazo de roles:

```json
{
  "roles": ["VETERINARIO", "PELUQUERO"]
}
```

Reglas principales:

- El correo se normaliza con `trim` y minusculas, y es unico sin distinguir mayusculas.
- La password inicial se valida con la politica existente y se guarda solo con BCrypt.
- Los roles enviados deben existir, estar activos y no repetirse.
- Siempre debe quedar al menos un administrador activo.
- Un administrador no puede desactivar su propia cuenta.
- No existe `DELETE` fisico de trabajadores.
- Las respuestas no exponen `passwordHash`, codigos de recuperacion ni credenciales.

Rutas sugeridas para Postman, carpeta `02 - Usuarios`:

- `POST - Registrar trabajador`
- `GET - Listar trabajadores`
- `GET - Obtener trabajador`
- `PUT - Editar trabajador`
- `PATCH - Cambiar estado`
- `PUT - Actualizar roles`
- `GET - Listar roles`

## Ejecucion

```powershell
.\mvnw.cmd spring-boot:run
```

## Pruebas

```powershell
docker version
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
