package com.veterinaria.backend.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(@NotNull Boolean activo) {
}
