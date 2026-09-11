# Especificación: Atención de peluquería y evidencias

## Alcance

Implementar atención de peluquería para citas `PELUQUERIA`, carga de evidencias fotográficas y cierre controlado de la cita.

## Reglas

- Solo una cita `PELUQUERIA` `CONFIRMADA` puede iniciar atención.
- El actor debe ser usuario activo, tener `PELUQUERO` y coincidir con `trabajadorAsignado` mediante `uid` JWT.
- La atención se crea sin cambiar el estado de la cita.
- Una evidencia contiene tipo, URL, nombre opcional y `storageKey` opcional para históricos.
- Nuevas evidencias usan Cloudinary y deben persistir `storageKey`.
- Se permiten `ANTES`, `DESPUES` y `FINAL`, múltiples veces.
- El cierre exige al menos una evidencia y cambia la cita a `ATENDIDA`.
- No existen operaciones de edición o eliminación de evidencias.
- El cierre genérico de citas rechaza citas médicas y de peluquería.

## Persistencia aprobada

Se agrega únicamente `fotos_peluqueria.storage_key VARCHAR(500) NULL`, compatible con registros históricos.

## Storage

La lógica de negocio depende de `ImageStorageService`, no del SDK. La implementación Cloudinary devuelve `secure_url` y `public_id`; este último se guarda como `storageKey`. Si PostgreSQL falla después del upload, se intenta eliminar el asset.
