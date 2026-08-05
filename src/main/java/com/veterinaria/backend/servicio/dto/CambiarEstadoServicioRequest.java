package com.veterinaria.backend.servicio.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoServicioRequest(@NotNull Boolean activo) {
}
