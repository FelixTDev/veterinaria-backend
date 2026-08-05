package com.veterinaria.backend.usuario.dto;

import java.util.List;

public record UsuarioResumenResponse(
        Long id,
        String nombreCompleto,
        String correo,
        String telefono,
        Boolean activo,
        List<String> roles) {
}
