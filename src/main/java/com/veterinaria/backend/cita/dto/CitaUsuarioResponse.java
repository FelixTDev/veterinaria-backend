package com.veterinaria.backend.cita.dto;

public record CitaUsuarioResponse(
        Long id,
        String nombreCompleto,
        String correo) {
}
