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

## Gestion de clientes y mascotas

Los módulos de clientes y mascotas requieren un Bearer JWT con rol `ADMINISTRADOR` o `RECEPCIONISTA`. Los roles `VETERINARIO` y `PELUQUERO` reciben 403. Las operaciones no eliminan físicamente información.

### Clientes

- `POST /api/v1/clientes`: registra un cliente activo.
- `GET /api/v1/clientes`: lista con `page`, `size`, `sort`, `search`, `activo` y `tipoDocumento`.
- `GET /api/v1/clientes/{id}`: obtiene datos y mascotas resumidas.
- `PUT /api/v1/clientes/{id}`: edita todos los datos personales editables.
- `PATCH /api/v1/clientes/{id}/estado`: activa o desactiva sin eliminar.
- `GET /api/v1/clientes/{id}/mascotas`: lista mascotas del cliente con paginación, `search` y `activo`.

Ejemplo de alta:

```json
{
  "primerNombre": "Maria",
  "segundoNombre": null,
  "primerApellido": "Torres",
  "segundoApellido": "Lopez",
  "tipoDocumento": "DNI",
  "numeroDocumento": "72845612",
  "fechaNacimiento": "1990-05-14",
  "telefono": "987654321",
  "correo": "maria.torres@gmail.com"
}
```

El documento es único por `tipoDocumento + numeroDocumento`. El correo se normaliza a minúsculas, pero no es único porque el esquema no define esa restricción.

### Mascotas

- `POST /api/v1/mascotas`: registra una mascota activa para un cliente activo.
- `GET /api/v1/mascotas`: lista con `page`, `size`, `sort`, `search`, `activo`, `clienteId`, `especie` y `sexo`.
- `GET /api/v1/mascotas/{id}`: obtiene el detalle y propietario resumido.
- `PUT /api/v1/mascotas/{id}`: edita los datos propios; no cambia el propietario.
- `PATCH /api/v1/mascotas/{id}/estado`: activa o desactiva sin eliminar.

Ejemplo de alta:

```json
{
  "clienteId": 25,
  "nombre": "Luna",
  "especie": "PERRO",
  "raza": "Labrador",
  "color": "Dorado",
  "sexo": "HEMBRA",
  "pesoKg": 24.5,
  "fechaNacimiento": "2022-05-14",
  "edadAproximadaAnios": 4
}
```

La fecha de nacimiento no puede ser futura y el peso, cuando se informa, debe ser mayor que cero. `edadAproximadaAnios` es un valor persistido existente: se expone si tiene valor, no se calcula ni se sincroniza automáticamente con la fecha de nacimiento. Su semántica queda pendiente para una feature futura.

No se aceptan `direccion`, `tamano`, `numeroMicrochip` ni `observaciones` porque no existen en las entidades ni en el esquema actual. No se implementa cambio de propietario.

Los listados usan consultas paginadas en PostgreSQL. El conteo de mascotas por cliente se obtiene mediante una consulta agregada por lote y el propietario de cada mascota se carga con `EntityGraph`, evitando consultas dentro de ciclos y N+1.

Las respuestas de validación son 400, los recursos inexistentes 404, los documentos duplicados o propietarios inactivos 409, las solicitudes sin token 401 y los roles no autorizados 403.

## Servicios, precios y programación

Catálogo:

- `POST /api/v1/servicios`: crea un servicio. Solo `ADMINISTRADOR`.
- `GET /api/v1/servicios` y `GET /api/v1/servicios/{id}`: consulta paginada o detalle. `ADMINISTRADOR`, `RECEPCIONISTA`, `VETERINARIO` y `PELUQUERO`.
- `PUT /api/v1/servicios/{id}` y `PATCH /api/v1/servicios/{id}/estado`: edición completa y estado. Solo `ADMINISTRADOR`.
- `GET /api/v1/servicios/{id}/precios` y `PUT /api/v1/servicios/{id}/precios`: consulta para los cuatro roles y configuración solo administrativa.

Programación:

- `POST /api/v1/trabajadores/{trabajadorId}/horarios`: crea una jornada.
- `GET /api/v1/trabajadores/{trabajadorId}/horarios` y `GET /api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}`: consulta general para administración/recepción o únicamente propia para veterinarios/peluqueros.
- `PUT /api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}` y `PATCH /api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}/disponibilidad`: gestión administrativa.
- `POST`, `GET`, `GET/{id}` y `PUT/{id}` bajo `/api/v1/trabajadores/{trabajadorId}/indisponibilidades`: gestión administrativa y lectura general/propia con la misma regla.
- `GET /api/v1/trabajadores/{trabajadorId}/disponibilidad?inicio=...&fin=...`: consulta disponibilidad base.

Solo son trabajadores programables los usuarios activos con rol `VETERINARIO` o `PELUQUERO`. Un administrador requiere además uno de esos roles; recepción y administración sin rol programable reciben 409. Un trabajador puede tener una sola jornada por día, el descanso es opcional y debe estar dentro de la jornada. No se implementan jornadas partidas, citas en el cálculo de disponibilidad, eliminación/cancelación de indisponibilidades ni `DELETE`.

Los solapamientos se consultan en PostgreSQL con intervalos semiabiertos: `[inicio, fin)`, por lo que intervalos contiguos no se solapan. Las respuestas usan DTOs y las consultas de listados cargan la relación de usuario con `EntityGraph`; los precios se cuentan por lote para evitar N+1. La duración de `Servicio` y `PrecioServicioTamano` se conserva independiente. No se agregan moneda, sede, clasificación de trabajador ni cambios al esquema.

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
