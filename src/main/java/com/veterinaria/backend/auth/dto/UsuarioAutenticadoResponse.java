package com.veterinaria.backend.auth.dto;

import java.util.List;

public record UsuarioAutenticadoResponse(
        Long id,
        String nombreCompleto,
        String correo,
        List<String> roles) {
}
