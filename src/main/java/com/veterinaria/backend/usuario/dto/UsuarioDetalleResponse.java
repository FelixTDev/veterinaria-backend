package com.veterinaria.backend.usuario.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioDetalleResponse(
        Long id,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        String correo,
        String telefono,
        Boolean activo,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime ultimoAcceso) {
}
