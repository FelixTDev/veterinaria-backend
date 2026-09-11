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

## Gestion de citas

Ruta base: `/api/v1/citas`.

- `POST /api/v1/citas`: crea una cita. Solo administración y recepción.
- `GET /api/v1/citas`: listado paginado y filtrado.
- `GET /api/v1/citas/disponibilidad`: combina jornada/descanso/indisponibilidad con citas ocupantes.
- `GET /api/v1/citas/{id}`: detalle con cliente, mascota, trabajador y servicios congelados.
- `PATCH /api/v1/citas/{id}/confirmar`
- `PATCH /api/v1/citas/{id}/reprogramar`
- `PATCH /api/v1/citas/{id}/cancelar`
- `PATCH /api/v1/citas/{id}/no-atendida`
- `PATCH /api/v1/citas/{id}/atendida`

La creación recibe únicamente `mascotaId`, `trabajadorId`, `fechaHoraInicio`, servicios, motivo y observaciones. El cliente se obtiene desde la mascota y `registradoPor` se obtiene exclusivamente del claim JWT `uid`. El frontend no envía tipo, fin, precio ni duración.

```json
{
  "mascotaId": 25,
  "trabajadorId": 8,
  "fechaHoraInicio": "2026-09-10T10:00:00",
  "servicios": [
    {"servicioId": 3, "precioServicioTamanoId": 11}
  ],
  "motivoConsulta": "Control anual",
  "observaciones": null
}
```

Cada `CitaServicio` congela `precioAplicado` y `duracionAplicadaMinutos` desde el catálogo del backend. Si se indica tarifa, debe estar activa y pertenecer al servicio; sin tarifa se usan precio base y duración del servicio. Reprogramar conserva esas instantáneas históricas y calcula el nuevo fin sumando sus duraciones.

Los servicios médicos solo admiten trabajadores activos con rol `VETERINARIO`; peluquería requiere `PELUQUERO`. No se mezclan ambos tipos en una cita y ninguna cita puede cruzar medianoche.

Estados permitidos:

- `PENDIENTE -> CONFIRMADA`, `CANCELADA` o `NO_ATENDIDA`.
- `CONFIRMADA -> ATENDIDA`, `CANCELADA` o `NO_ATENDIDA`.
- `ATENDIDA`, `NO_ATENDIDA` y `CANCELADA` son terminales.

`ATENDIDA` y `NO_ATENDIDA` requieren que el inicio haya llegado. Cancelación y no atención requieren motivo. Una cancelación conserva la cita y sus servicios, pero libera el intervalo porque solo `PENDIENTE` y `CONFIRMADA` ocupan agenda.

Permisos:

- Administración y recepción crean, consultan, confirman, reprograman, cancelan y marcan no atendida.
- Recepción no puede marcar atendida.
- Administración y el profesional asignado compatible pueden marcar atendida en peluquería. Las citas médicas se cierran únicamente al registrar atención médica.
- Veterinarios y peluqueros solo consultan y operan citas propias compatibles. En listados y reprogramación el backend fuerza su `uid`, aunque se envíe otro `trabajadorId`.

Filtros de listado: `fechaHoraInicioDesde`, `fechaHoraInicioHasta`, `estado`, `tipoCita`, `clienteId`, `mascotaId`, `trabajadorId`, `busqueda`, `page`, `size` y `sort`. Las fechas son ISO-8601. El sort acepta únicamente `id`, `fechaHoraInicio`, `fechaHoraFin`, `estado`, `tipoCita`, `createdAt` y `updatedAt`, con dirección `asc` o `desc`.

La disponibilidad responde `disponibilidadBase`, `tieneCitaSolapada`, `disponible` y `motivo`. Los intervalos son semiabiertos, por lo que dos citas contiguas son válidas. Creación bloquea primero al trabajador; reprogramación bloquea cita y luego trabajador; las transiciones bloquean la cita. Este locking reduce carreras dentro de la aplicación, pero el esquema actual no tiene una restricción de exclusión PostgreSQL: ese refuerzo a nivel base de datos queda fuera del alcance actual.

## Atención médica

Endpoints clínicos:

- `POST /api/v1/citas/{citaId}/atencion-medica`: registra una atención para una cita médica `CONFIRMADA` y cambia la cita a `ATENDIDA` en la misma transacción.
- `GET /api/v1/citas/{citaId}/atencion-medica`: obtiene el detalle clínico.
- `GET /api/v1/mascotas/{mascotaId}/atenciones-medicas`: historial paginado, ordenado por `fechaAtencion DESC, id DESC`, con filtros opcionales `desde` y `hasta`.

El request utiliza únicamente `pesoKg`, `temperaturaC`, `sintomas`, `diagnostico`, `motivoSinDiagnostico`, `tratamiento`, `motivoSinTratamiento`, `receta`, `motivoSinReceta` y `observaciones`. Diagnóstico, tratamiento y receta requieren su valor o el motivo correspondiente. El veterinario se deriva del `uid` JWT y del trabajador asignado; no se acepta `veterinarioId`.

Las atenciones son inmutables mediante API: no tienen `PUT`, `PATCH` ni `DELETE`. Veterinarios y administradores pueden leer información clínica; recepción y peluquería no pueden acceder a endpoints clínicos. Un administrador solo puede crear si además tiene rol `VETERINARIO` y está asignado a la cita.

La creación bloquea la cita con `PESSIMISTIC_WRITE` y aprovecha `UNIQUE(cita_id)` para impedir duplicados concurrentes.

## Vacunas y vacunas aplicadas

Catálogo: `POST /api/v1/vacunas`, `GET /api/v1/vacunas`, `GET /api/v1/vacunas/{id}`, `PUT /api/v1/vacunas/{id}` y `PATCH /api/v1/vacunas/{id}/estado`. Escribir requiere `ADMINISTRADOR`; leer requiere `ADMINISTRADOR` o `VETERINARIO`. No existe DELETE.

Aplicaciones: `POST /api/v1/atenciones-medicas/{atencionId}/vacunas` y `GET /api/v1/atenciones-medicas/{atencionId}/vacunas`. El historial paginado está en `GET /api/v1/mascotas/{mascotaId}/vacunas-aplicadas`, con filtros `desde`, `hasta` y `vacunaId`. Solo administrador y veterinario pueden consultar endpoints clínicos.

`fechaAplicacion` se deriva de la atención médica. `VacunaAplicada` es inmutable mediante API. La unicidad case-insensitive del catálogo se valida en aplicación, pero el UNIQUE PostgreSQL existente es case-sensitive y conserva una carrera residual entre escrituras concurrentes con distinta capitalización.

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
