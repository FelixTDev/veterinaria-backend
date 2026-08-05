package com.veterinaria.backend.usuario.dto;

import java.util.List;

public record UsuarioRolesResponse(Long id, String correo, List<String> roles) {
}
