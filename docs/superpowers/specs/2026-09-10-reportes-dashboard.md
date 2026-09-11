# Especificación: Reportes y Dashboard

## Alcance

El módulo `reporte` expone métricas agregadas calculadas principalmente en PostgreSQL. No devuelve entidades JPA ni datos clínicos sensibles.

Endpoints:

- `GET /api/v1/dashboard/resumen`
- `GET /api/v1/reportes/citas/resumen`
- `GET /api/v1/reportes/citas/tendencia`
- `GET /api/v1/reportes/servicios/mas-solicitados`
- `GET /api/v1/reportes/finanzas/resumen`
- `GET /api/v1/reportes/pagos/saldos-pendientes`
- `GET /api/v1/reportes/comprobantes/resumen`
- `GET /api/v1/reportes/vacunas/resumen`

## Tiempo

Los endpoints temporales requieren `desde` y `hasta` como `LocalDateTime`. El intervalo es `[desde, hasta)`: desde inclusivo y hasta exclusivo. No se interpreta automáticamente “hoy” y no se introduce una zona horaria global en este módulo.

Las citas se filtran por `fecha_hora_inicio`; pagos por `fecha_pago`; comprobantes por `fecha_emision`; atenciones por `fecha_atencion`; vacunas por `fecha_aplicacion`.

## Indicadores

- Ingreso: suma de `pagos.monto_total` con estado `PAGADO` y `fecha_pago` dentro del rango.
- Ingreso por medio: suma de `detalle_pagos.monto` unido a pagos `PAGADO`. No se suma junto con `monto_total`.
- Saldo: por cada cita `ATENDIDA`, servicios históricos menos pagos `PAGADO`; el saldo global suma únicamente saldos positivos.
- Citas: conteos separados por estado y tipo.
- Servicios: conteo de `CitaServicio` en citas `ATENDIDA`, ordenado por cantidad descendente e id ascendente.
- Comprobantes emitidos: solo estado `EMITIDO`, separados por boleta y factura.
- Vacunas: aplicaciones por `fecha_aplicacion`, conservando vacunas inactivas en el histórico.
- Atenciones: se cuentan registros reales de `AtencionMedica` y `AtencionPeluqueria`.

## Seguridad

- Dashboard, finanzas, saldos, comprobantes y ranking de servicios: `ADMINISTRADOR`, `RECEPCIONISTA`.
- Reportes de citas: `ADMINISTRADOR`, `RECEPCIONISTA`, `VETERINARIO`.
- Reportes de vacunas: `ADMINISTRADOR`, `VETERINARIO`.
- `PELUQUERO`: 403 en todos los endpoints de esta primera versión.

## Limitaciones

- No existe timestamp histórico de transición a `ATENDIDA`; los saldos usan `Cita.fechaHoraInicio`.
- `CitaServicio` no conserva snapshot del nombre del servicio; los rankings usan el nombre actual.
- PostgreSQL no tiene aún índices específicos para todas las fechas y estados de reportes. No se agregan índices en esta feature.
