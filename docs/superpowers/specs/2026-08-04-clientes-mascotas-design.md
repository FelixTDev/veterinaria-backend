# Gestion de Clientes y Mascotas

## Alcance

Implementar la gestion administrativa de clientes y mascotas sobre las entidades y tablas existentes, sin cambios de esquema, entidades JPA ni dependencias Maven.

Los endpoints seran accesibles para `ADMINISTRADOR` y `RECEPCIONISTA` mediante `@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")`. `VETERINARIO` y `PELUQUERO` recibiran 403; las solicitudes sin autenticacion valida recibiran 401 por la configuracion stateless existente.

## Modelo real y decisiones

`Cliente` usa primer/segundo nombre, primer/segundo apellido, tipo y numero de documento, fecha de nacimiento, telefono, correo y activo. La unicidad es `(tipo_documento, numero_documento)`. El correo es opcional y no unico.

`Mascota` usa cliente, nombre, especie, raza, color, sexo, pesoKg, fechaNacimiento, edadAproximadaAnios y activo. El propietario es obligatorio mediante `cliente_id` y no se cambia en la edicion. No se expondran ni aceptaran direccion, tamano, microchip u observaciones porque no existen en el modelo.

`edadAproximadaAnios` se conserva como columna y propiedad persistida. Se expone solo cuando ya tenga valor, no se calcula, no se sincroniza con `fechaNacimiento` y no es obligatoria en altas o ediciones. Queda pendiente definir su semantica en una feature futura.

## API

Clientes:

- `POST /api/v1/clientes`
- `GET /api/v1/clientes?page=0&size=20&search=&activo=&tipoDocumento=`
- `GET /api/v1/clientes/{id}`
- `PUT /api/v1/clientes/{id}`
- `PATCH /api/v1/clientes/{id}/estado`
- `GET /api/v1/clientes/{id}/mascotas?page=0&size=20&search=&activo=`

Mascotas:

- `POST /api/v1/mascotas`
- `GET /api/v1/mascotas?page=0&size=20&search=&activo=&clienteId=&especie=&sexo=`
- `GET /api/v1/mascotas/{id}`
- `PUT /api/v1/mascotas/{id}`
- `PATCH /api/v1/mascotas/{id}/estado`

Todas las respuestas usan DTOs y la paginacion reutiliza `PaginaResponse<T>`.

## Capas

`ClienteController` y `MascotaController` solo validan requests y delegan en `ClienteGestionService` y `MascotaGestionService`. Los servicios aplican normalizacion, duplicados, existencia/estado del propietario, estados activos, transacciones y mapeo. Los mappers manuales no consultan repositorios.

Las consultas de listados se ejecutan en PostgreSQL con paginacion y filtros. Los propietarios de mascotas se cargan mediante proyeccion o `join fetch`. El conteo de mascotas de clientes se obtiene mediante una consulta agregada o batch, nunca con una consulta por cliente.

## Errores

- 400: Bean Validation, fecha futura o peso no positivo.
- 404: cliente o mascota inexistente.
- 409: documento duplicado o alta de mascota para cliente inactivo.
- 401/403: gestionados por la seguridad existente.

Las excepciones se integraran en `GlobalExceptionHandler` sin exponer SQL, stack traces ni datos sensibles.

## Pruebas

Se agregaran pruebas unitarias Mockito para ambos servicios y mappers. Las pruebas de integracion usaran MockMvc y el Testcontainer PostgreSQL existente, cubriendo seguridad, CRUD administrativo, filtros, paginacion, restricciones de propietario, estados, errores y ausencia de ciclos o campos internos. No se usara H2 ni `veterinaria_db` durante las pruebas.

## Fuera de alcance

No se implementan eliminacion fisica, cambio de propietario, historia clinica, citas, servicios, pagos, campos no presentes en el esquema, migraciones ni cambios de seguridad/autenticacion.
