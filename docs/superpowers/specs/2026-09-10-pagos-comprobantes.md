# Especificación: Pagos y comprobantes

## Pagos

Un pago se crea directamente como `PAGADO` para una cita `ATENDIDA`. El monto se calcula como la suma de sus detalles, y el saldo usa `SUM(pagos PAGADO)` contra `SUM(cita_servicios.precio_aplicado)`. La cita se bloquea durante la validación y persistencia para evitar sobrepagos concurrentes. Administrador y recepcionista tienen acceso; el recurso es inmutable por API.

## Comprobantes

Solo se emiten para pagos `PAGADO`, siempre como `EMITIDO`. Serie y número son manuales y se apoyan en `UNIQUE(pago_id)` y `UNIQUE(serie,numero)`; una carrera se traduce a 409. Factura valida RUC, razón social y dirección fiscal sin servicios externos. PDF, SUNAT y anulación quedan fuera de alcance.
