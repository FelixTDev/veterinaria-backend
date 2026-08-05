# Servicios, Horarios e Indisponibilidades

## Alcance

Implementar el catalogo de servicios, precios por tamano, horarios de trabajadores, indisponibilidades y disponibilidad base sin modificar entidades JPA, esquema PostgreSQL, migraciones ni dependencias.

Los trabajadores programables son exclusivamente usuarios activos con al menos uno de los roles `VETERINARIO` o `PELUQUERO`. Un usuario con `ADMINISTRADOR` tambien necesita uno de esos roles. `RECEPCIONISTA` y `ADMINISTRADOR` sin rol programable no pueden recibir horarios ni indisponibilidades.

## Modelo real

`Servicio` contiene nombre unico, tipo (`MEDICO` o `PELUQUERIA`), descripcion, precio base opcional, duracion obligatoria y activo. `PrecioServicioTamano` contiene servicio, tamano (`PEQUENO`, `MEDIANO`, `GRANDE`), precio, duracion y activo; la unicidad es servicio + tamano.

`HorarioTrabajador` contiene usuario, dia numerico 1-7, inicio, fin, descanso opcional y `disponible`. La base impone una sola fila por trabajador y dia, por lo que no se implementan jornadas partidas. El descanso debe estar dentro de la jornada.

`IndisponibilidadTrabajador` contiene usuario, `fechaInicio`, `fechaFin` y motivo. No tiene estado ni `updatedAt`, por lo que no se implementan cancelacion, desactivacion ni DELETE. Solo se crean y editan periodos futuros.

## API

Servicios y precios:

- `POST /api/v1/servicios`
- `GET /api/v1/servicios`
- `GET /api/v1/servicios/{id}`
- `PUT /api/v1/servicios/{id}`
- `PATCH /api/v1/servicios/{id}/estado`
- `GET /api/v1/servicios/{servicioId}/precios`
- `PUT /api/v1/servicios/{servicioId}/precios`

Horarios:

- `POST /api/v1/trabajadores/{trabajadorId}/horarios`
- `GET /api/v1/trabajadores/{trabajadorId}/horarios`
- `GET /api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}`
- `PUT /api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}`
- `PATCH /api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}/estado`

Indisponibilidades:

- `POST /api/v1/trabajadores/{trabajadorId}/indisponibilidades`
- `GET /api/v1/trabajadores/{trabajadorId}/indisponibilidades`
- `GET /api/v1/trabajadores/{trabajadorId}/indisponibilidades/{id}`
- `PUT /api/v1/trabajadores/{trabajadorId}/indisponibilidades/{id}`

Disponibilidad:

- `GET /api/v1/trabajadores/{trabajadorId}/disponibilidad?inicio=...&fin=...`

## Autorizacion

El catalogo y precios usan `hasRole('ADMINISTRADOR')` para escritura y `hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'VETERINARIO', 'PELUQUERO')` para lectura.

Horarios, indisponibilidades y disponibilidad usan `ADMINISTRADOR` para gestion, `ADMINISTRADOR` y `RECEPCIONISTA` para consulta general, y permiten a `VETERINARIO`/`PELUQUERO` consultar solo su propio `trabajadorId`, comparando el `uid` del JWT en el service. Nunca se permite acceso de esos roles a programación ajena.

## Reglas y disponibilidad

Los solapamientos se detectan en PostgreSQL con `inicioExistente < finNuevo AND finExistente > inicioNuevo`, excluyendo el propio id en ediciones. Los intervalos contiguos no se solapan. La unicidad diaria del esquema impide registrar dos filas contiguas el mismo día.

La disponibilidad requiere un horario `disponible` que cubra completamente el intervalo, sin cruzar el descanso, y ninguna indisponibilidad superpuesta. No se consultan citas, servicios, feriados, salas ni sobrecupos.

## Rendimiento y errores

Los listados son paginados en PostgreSQL. Los precios se cargan con consultas batch o una consulta por servicio en detalle; no se consultan dentro de ciclos. Horarios, indisponibilidades y disponibilidad usan consultas directas `exists`/ordenadas. Las respuestas son DTOs y reutilizan `PaginaResponse<T>`.

Se integran errores 400, 404 y 409 con `GlobalExceptionHandler`; 401 y 403 permanecen bajo la seguridad existente. Las consultas de existencia previas a escrituras de indisponibilidad tienen un riesgo de carrera porque el esquema no posee una restriccion de exclusion; se documentara sin agregar locking ni cambios de esquema.

## Pruebas

Se agregaran pruebas unitarias Mockito para servicios y mappers, y pruebas MockMvc con Testcontainers PostgreSQL para seguridad, roles programables, catalogo, precios, rangos, solapamientos, propiedad de rutas y disponibilidad. No se usara H2 ni `veterinaria_db` en pruebas automatizadas.
