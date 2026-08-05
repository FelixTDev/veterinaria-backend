package com.veterinaria.backend.cliente.dto;

import com.veterinaria.backend.cliente.enums.TipoDocumento;

public record ClienteResumenResponse(
        Long id,
        String nombreCompleto,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String correo,
        String telefono,
        Boolean activo,
        long cantidadMascotas) {
}
