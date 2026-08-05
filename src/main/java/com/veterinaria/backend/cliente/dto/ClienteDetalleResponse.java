package com.veterinaria.backend.cliente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.cliente.enums.TipoDocumento;

public record ClienteDetalleResponse(
        Long id,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        Boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MascotaClienteResumenResponse> mascotas) {
}
