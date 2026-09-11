# Contrato API v1

Todos los endpoints usan JSON salvo la carga de evidencias. Las rutas protegidas
requieren `Authorization: Bearer <accessToken>`; `page` es zero-based y los
listados aceptan `size` entre 1 y 100. `sort` solo admite propiedades documentadas
por cada listado. Las respuestas de error mantienen los campos históricos y
añaden `code` y `fieldErrors`.

## Endpoints

| Método | Ruta | Acceso | Request / response y notas |
|---|---|---|---|
| GET | `/api/v1/health` | Público | — / estado |
| POST | `/api/v1/auth/login` | Público | `LoginRequest` / `LoginResponse`; devuelve access JWT |
| GET | `/api/v1/auth/me` | Autenticado | — / `UsuarioAutenticadoResponse` |
| POST | `/api/v1/auth/cambiar-password` | Autenticado | `CambiarPasswordRequest` / mensaje |
| POST | `/api/v1/auth/recuperacion/solicitar` | Público | `SolicitarRecuperacionRequest` / mensaje genérico |
| POST | `/api/v1/auth/recuperacion/validar-codigo` | Público | `ValidarCodigoRequest` / recovery token |
| POST | `/api/v1/auth/recuperacion/restablecer` | Público | `RestablecerPasswordRequest` / mensaje |
| POST | `/api/v1/usuarios` | ADMINISTRADOR | `CrearUsuarioRequest` / detalle, 201 |
| GET | `/api/v1/usuarios` | ADMINISTRADOR | filtros `search,activo,rol,page,size,sort` / página |
| GET | `/api/v1/usuarios/{id}` | ADMINISTRADOR | — / detalle |
| PUT | `/api/v1/usuarios/{id}` | ADMINISTRADOR | `ActualizarUsuarioRequest` / detalle |
| PATCH | `/api/v1/usuarios/{id}/estado` | ADMINISTRADOR | `CambiarEstadoUsuarioRequest` / detalle |
| PUT | `/api/v1/usuarios/{id}/roles` | ADMINISTRADOR | `ActualizarRolesUsuarioRequest` / roles |
| GET | `/api/v1/roles` | ADMINISTRADOR | — / roles activos |
| POST | `/api/v1/clientes` | ADMINISTRADOR, RECEPCIONISTA | `CrearClienteRequest` / detalle, 201 |
| GET | `/api/v1/clientes` | ADMINISTRADOR, RECEPCIONISTA | filtros `search,activo,tipoDocumento,page,size,sort` / página |
| GET | `/api/v1/clientes/{id}` | ADMINISTRADOR, RECEPCIONISTA | — / detalle |
| PUT | `/api/v1/clientes/{id}` | ADMINISTRADOR, RECEPCIONISTA | `ActualizarClienteRequest` / detalle |
| PATCH | `/api/v1/clientes/{id}/estado` | ADMINISTRADOR, RECEPCIONISTA | `CambiarEstadoClienteRequest` / detalle |
| GET | `/api/v1/clientes/{id}/mascotas` | ADMINISTRADOR, RECEPCIONISTA | `search,activo,page,size,sort` / página |
| POST | `/api/v1/mascotas` | ADMINISTRADOR, RECEPCIONISTA | `CrearMascotaRequest` / detalle, 201 |
| GET | `/api/v1/mascotas` | ADMINISTRADOR, RECEPCIONISTA | filtros `search,activo,clienteId,especie,sexo,page,size,sort` / página |
| GET | `/api/v1/mascotas/{id}` | ADMINISTRADOR, RECEPCIONISTA | — / detalle |
| PUT | `/api/v1/mascotas/{id}` | ADMINISTRADOR, RECEPCIONISTA | `ActualizarMascotaRequest` / detalle |
| PATCH | `/api/v1/mascotas/{id}/estado` | ADMINISTRADOR, RECEPCIONISTA | `CambiarEstadoMascotaRequest` / detalle |
| POST | `/api/v1/servicios` | ADMINISTRADOR | `CrearServicioRequest` / detalle, 201 |
| GET | `/api/v1/servicios` | ADMINISTRADOR, RECEPCIONISTA, VETERINARIO, PELUQUERO | `search,activo,tipoServicio,page,size,sort` / página |
| GET | `/api/v1/servicios/{id}` | ADMINISTRADOR, RECEPCIONISTA, VETERINARIO, PELUQUERO | — / detalle |
| PUT | `/api/v1/servicios/{id}` | ADMINISTRADOR | `ActualizarServicioRequest` / detalle |
| PATCH | `/api/v1/servicios/{id}/estado` | ADMINISTRADOR | `CambiarEstadoServicioRequest` / detalle |
| GET | `/api/v1/servicios/{id}/precios` | roles de lectura de servicios | — / precios |
| PUT | `/api/v1/servicios/{id}/precios` | ADMINISTRADOR | `ConfigurarPreciosRequest` / precios |
| POST | `/api/v1/trabajadores/{trabajadorId}/horarios` | ADMINISTRADOR | `CrearHorarioRequest` / horario, 201 |
| GET | `/api/v1/trabajadores/{trabajadorId}/horarios` | ADMINISTRADOR, RECEPCIONISTA o propio profesional | — / horarios |
| GET | `/api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}` | ADMINISTRADOR, RECEPCIONISTA o propio profesional | — / horario |
| PUT | `/api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}` | ADMINISTRADOR | `ActualizarHorarioRequest` / horario |
| PATCH | `/api/v1/trabajadores/{trabajadorId}/horarios/{horarioId}/disponibilidad` | ADMINISTRADOR | `CambiarDisponibilidadRequest` / horario |
| POST | `/api/v1/trabajadores/{trabajadorId}/indisponibilidades` | ADMINISTRADOR | `CrearIndisponibilidadRequest` / indisponibilidad, 201 |
| GET | `/api/v1/trabajadores/{trabajadorId}/indisponibilidades` | ADMINISTRADOR, RECEPCIONISTA o propio profesional | — / lista |
| GET | `/api/v1/trabajadores/{trabajadorId}/indisponibilidades/{id}` | ADMINISTRADOR, RECEPCIONISTA o propio profesional | — / detalle |
| PUT | `/api/v1/trabajadores/{trabajadorId}/indisponibilidades/{id}` | ADMINISTRADOR | `ActualizarIndisponibilidadRequest` / detalle |
| GET | `/api/v1/trabajadores/{trabajadorId}/disponibilidad` | ADMINISTRADOR, RECEPCIONISTA o propio profesional | `inicio,fin` / disponibilidad |
| POST | `/api/v1/citas` | ADMINISTRADOR, RECEPCIONISTA | `CrearCitaRequest` / cita, 201 |
| GET | `/api/v1/citas` | roles operativos | filtros de cita, `page,size,sort` / página |
| GET | `/api/v1/citas/disponibilidad` | roles operativos | trabajador/tipo/inicio/fin / disponibilidad |
| GET | `/api/v1/citas/{id}` | roles operativos | — / detalle |
| PATCH | `/api/v1/citas/{id}/confirmar` | roles operativos | — / cita |
| PATCH | `/api/v1/citas/{id}/reprogramar` | roles operativos | `ReprogramarCitaRequest` / cita |
| PATCH | `/api/v1/citas/{id}/cancelar` | roles operativos | `CancelarCitaRequest` / cita |
| PATCH | `/api/v1/citas/{id}/no-atendida` | roles operativos | `MarcarNoAtendidaRequest` / cita |
| PATCH | `/api/v1/citas/{id}/atendida` | ADMINISTRADOR, VETERINARIO, PELUQUERO | flujo protegido por regla de dominio |
| POST | `/api/v1/citas/{citaId}/atencion-medica` | VETERINARIO responsable | `CrearAtencionMedicaRequest` / atención |
| GET | `/api/v1/citas/{citaId}/atencion-medica` | ADMINISTRADOR, VETERINARIO | — / atención |
| GET | `/api/v1/mascotas/{mascotaId}/atenciones-medicas` | ADMINISTRADOR, VETERINARIO | `desde,hasta,page,size` / página |
| POST | `/api/v1/vacunas` | ADMINISTRADOR | `CrearVacunaRequest` / vacuna |
| GET | `/api/v1/vacunas` | ADMINISTRADOR, VETERINARIO | `activo,page,size,sort` / página |
| GET | `/api/v1/vacunas/{id}` | ADMINISTRADOR, VETERINARIO | — / vacuna |
| PUT | `/api/v1/vacunas/{id}` | ADMINISTRADOR | `ActualizarVacunaRequest` / vacuna |
| PATCH | `/api/v1/vacunas/{id}/estado` | ADMINISTRADOR | `CambiarEstadoVacunaRequest` / vacuna |
| POST | `/api/v1/atenciones-medicas/{atencionId}/vacunas` | VETERINARIO responsable | `AplicarVacunaRequest` / aplicación; fecha derivada |
| GET | `/api/v1/atenciones-medicas/{atencionId}/vacunas` | ADMINISTRADOR, VETERINARIO | — / aplicaciones |
| GET | `/api/v1/mascotas/{mascotaId}/vacunas-aplicadas` | ADMINISTRADOR, VETERINARIO | `desde,hasta,vacunaId,page,size` / página |
| POST | `/api/v1/citas/{citaId}/atencion-peluqueria` | PELUQUERO asignado | `CrearAtencionPeluqueriaRequest` / atención, 201 |
| GET | `/api/v1/citas/{citaId}/atencion-peluqueria` | ADMINISTRADOR, PELUQUERO | — / atención |
| POST | `/api/v1/atenciones-peluqueria/{id}/evidencias` | PELUQUERO asignado | multipart `archivo,tipoFoto`; imagen <=5MB / foto, 201 |
| GET | `/api/v1/atenciones-peluqueria/{id}/evidencias` | ADMINISTRADOR, PELUQUERO | — / fotos |
| PATCH | `/api/v1/atenciones-peluqueria/{id}/cerrar` | PELUQUERO asignado | — / atención cerrada |
| POST | `/api/v1/citas/{citaId}/pagos` | ADMINISTRADOR, RECEPCIONISTA | `RegistrarPagoRequest` / pago |
| GET | `/api/v1/citas/{citaId}/pagos` | ADMINISTRADOR, RECEPCIONISTA | `page,size,sort` / página |
| GET | `/api/v1/pagos/{id}` | ADMINISTRADOR, RECEPCIONISTA | — / pago |
| POST | `/api/v1/pagos/{pagoId}/comprobante` | ADMINISTRADOR, RECEPCIONISTA | `EmitirComprobanteRequest` / comprobante |
| GET | `/api/v1/pagos/{pagoId}/comprobante` | ADMINISTRADOR, RECEPCIONISTA | — / comprobante |
| GET | `/api/v1/comprobantes/{id}` | ADMINISTRADOR, RECEPCIONISTA | — / comprobante |
| GET | `/api/v1/dashboard/resumen` | ADMINISTRADOR, RECEPCIONISTA | rango opcional / dashboard |
| GET | `/api/v1/reportes/citas/resumen` | ADMINISTRADOR, RECEPCIONISTA, VETERINARIO | `desde,hasta` / resumen |
| GET | `/api/v1/reportes/citas/tendencia` | ADMINISTRADOR, RECEPCIONISTA, VETERINARIO | `desde,hasta,granularidad` / tendencia |
| GET | `/api/v1/reportes/finanzas/resumen` | ADMINISTRADOR, RECEPCIONISTA | `desde,hasta` / resumen |
| GET | `/api/v1/reportes/finanzas/pagos/saldos-pendientes` | ADMINISTRADOR, RECEPCIONISTA | `desde,hasta,page,size` / página |
| GET | `/api/v1/reportes/servicios/mas-solicitados` | ADMINISTRADOR, RECEPCIONISTA | `desde,hasta,limite` / ranking |
| GET | `/api/v1/reportes/comprobantes/resumen` | ADMINISTRADOR, RECEPCIONISTA | `desde,hasta` / resumen |
| GET | `/api/v1/reportes/vacunas/resumen` | ADMINISTRADOR, VETERINARIO | `desde,hasta` / ranking |

## Errores

```json
{
  "timestamp": "2026-09-11T10:00:00-05:00",
  "status": 400,
  "error": "Bad Request",
  "message": "correo: debe ser una dirección de correo válida",
  "path": "/api/v1/auth/login",
  "code": "VALIDATION_ERROR",
  "fieldErrors": {"correo": "debe ser una dirección de correo válida"}
}
```

Los códigos principales son `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`,
`RESOURCE_NOT_FOUND`, `CONFLICT`, `BUSINESS_RULE_VIOLATION`, `ACCOUNT_LOCKED`,
`SERVICE_UNAVAILABLE` e `INTERNAL_ERROR`. Los errores 500 solo exponen un
mensaje genérico.

## Convenciones Angular

- Autenticación stateless mediante `Authorization: Bearer <accessToken>`; no cookies.
- Fechas y horas usan ISO-8601; los reportes trabajan con intervalo `[desde,hasta)`.
- Evidencias usan `multipart/form-data`, campo `archivo` y `tipoFoto`; se aceptan JPEG, PNG y WebP hasta 5 MB.
- `APP_CORS_ALLOWED_ORIGINS` configura orígenes permitidos; ejemplo local: `http://localhost:4200`.
- La aplicación de vacuna no recibe `fechaAplicacion`; se deriva de `AtencionMedica.fechaAtencion`.
