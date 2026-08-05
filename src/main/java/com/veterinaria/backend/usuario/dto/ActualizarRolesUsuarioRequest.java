package com.veterinaria.backend.usuario.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ActualizarRolesUsuarioRequest(@NotEmpty List<@NotBlank String> roles) {
}
