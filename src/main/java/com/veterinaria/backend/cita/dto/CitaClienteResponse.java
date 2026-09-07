package com.veterinaria.backend.cita.dto;

public record CitaClienteResponse(
        Long id,
        String nombreCompleto,
        String telefono,
        String correo) {
}
