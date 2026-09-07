# Gestión de Citas

## Alcance

Implementar la gestión operativa completa de citas sobre las entidades y el esquema existentes, sin modificar entidades JPA, PostgreSQL, migraciones ni dependencias. La feature cubre creación, listado, detalle, disponibilidad, confirmación, reprogramación, cancelación y cierre como atendida o no atendida. No incluye atención médica, peluquería, pagos, comprobantes, notificaciones ni reportes.

## Modelo real

`Cita` persiste mascota, trabajador asignado, usuario registrador, tipo (`MEDICA` o `PELUQUERIA`), estado, inicio, fin, motivos, observaciones y auditoría. El cliente se obtiene por `Cita -> Mascota -> Cliente`; no se recibe `clienteId`.

`CitaServicio` persiste una relación única cita-servicio, una tarifa por tamaño opcional y las instantáneas obligatorias `precioAplicado` y `duracionAplicadaMinutos`. No existe cantidad. La cita no guarda duración total: `fechaHoraFin` se calcula sumando las duraciones aplicadas.

## Creación y resolución de servicios

`CrearCitaRequest` recibe `mascotaId`, `trabajadorId`, `fechaHoraInicio`, `motivoConsulta`, `observaciones` y una lista no vacía de `ServicioCitaRequest(servicioId, precioServicioTamanoId)`. No recibe tipo, fin, precio, duración, cliente ni registrador.

Los servicios deben existir, estar activos, no repetirse y pertenecer a un único `TipoServicio`. `MEDICO` se convierte en `TipoCita.MEDICA`; `PELUQUERIA`, en `TipoCita.PELUQUERIA`.

Si se informa una tarifa, debe existir, estar activa y pertenecer al servicio. Se congelan su precio y duración. Sin tarifa se usan `Servicio.precioBase` y `Servicio.duracionMinutos`; un precio base nulo produce conflicto. Un precio cero persistido es válido.

La mascota y su propietario deben estar activos. El trabajador se bloquea con `PESSIMISTIC_WRITE`, debe estar activo y tener `VETERINARIO` para citas médicas o `PELUQUERO` para peluquería. `registradoPor` se resuelve exclusivamente desde el `uid` del JWT.

## Agenda y concurrencia

Los estados ocupantes son exclusivamente `PENDIENTE` y `CONFIRMADA`. Un intervalo se solapa cuando `existente.inicio < nuevoFin AND existente.fin > nuevoInicio`; en reprogramación se excluye la cita actual. Los intervalos contiguos son válidos.

Después de adquirir el lock del trabajador se reutiliza `DisponibilidadTrabajadorService` para validar jornada, descanso e indisponibilidades, y luego se consulta el solapamiento de citas. No se admiten citas que crucen medianoche.

Creación bloquea primero el trabajador objetivo. Reprogramación bloquea primero la cita y después el trabajador objetivo. Las transiciones bloquean primero la cita. Este orden será uniforme. El locking reduce carreras dentro de la aplicación, pero no sustituye una exclusión PostgreSQL, fuera de alcance.

## Estados

Transiciones válidas:

- `PENDIENTE -> CONFIRMADA`
- `PENDIENTE -> CANCELADA`
- `PENDIENTE -> NO_ATENDIDA`
- `CONFIRMADA -> ATENDIDA`
- `CONFIRMADA -> CANCELADA`
- `CONFIRMADA -> NO_ATENDIDA`

`ATENDIDA`, `NO_ATENDIDA` y `CANCELADA` son terminales. Las transiciones repetidas o inválidas devuelven 409. `ATENDIDA` y `NO_ATENDIDA` requieren que el inicio haya llegado. Cancelación y no atención exigen motivo no vacío. Cancelar conserva la cita y sus servicios y libera el slot por estado.

Reprogramar solo cambia trabajador, inicio y fin. Conserva servicios, precios y duraciones históricas; el nuevo fin se calcula con la suma almacenada en `cita_servicios`.

## API y autorización

- `POST /api/v1/citas`
- `GET /api/v1/citas`
- `GET /api/v1/citas/disponibilidad`
- `GET /api/v1/citas/{id}`
- `PATCH /api/v1/citas/{id}/confirmar`
- `PATCH /api/v1/citas/{id}/reprogramar`
- `PATCH /api/v1/citas/{id}/cancelar`
- `PATCH /api/v1/citas/{id}/no-atendida`
- `PATCH /api/v1/citas/{id}/atendida`

Administrador y recepcionista crean, consultan, confirman, reprograman y cancelan. Ambos pueden marcar no atendida. Recepción no marca atendida. Administrador y el profesional asignado pueden marcar atendida. Veterinario y peluquero solo consultan y operan citas propias compatibles; el backend fuerza el `uid` del JWT y no confía en filtros arbitrarios.

## Consultas y rendimiento

`CitaRepository` usará `JpaSpecificationExecutor` para filtros PostgreSQL por fechas, estado, tipo, cliente, mascota, trabajador y búsqueda. El sort se reconstruirá desde una lista blanca.

La página carga únicamente relaciones `to-one` mediante `EntityGraph`. Los servicios se obtienen en una segunda consulta batch por los IDs de la página, con servicio y tarifa cargados, y se agrupan por cita. No se hace fetch join de colecciones paginadas ni consultas dentro de ciclos.

## Errores

- 400: request, rango, página o sort inválidos.
- 401: token ausente o inválido.
- 403: rol sin permiso o profesional intentando operar una cita ajena.
- 404: cita, mascota, trabajador, servicio o tarifa inexistente.
- 409: estado, tipo mixto, profesional incompatible, recurso inactivo, tarifa inutilizable, disponibilidad o solapamiento.

## Pruebas

Las pruebas unitarias usarán JUnit 5 y Mockito sin Spring. Las pruebas de integración usarán MockMvc, JWT real y Testcontainers PostgreSQL. Todas las fechas que representen futuro o pasado serán relativas a `LocalDateTime.now()` con segundos y nanos normalizados.

