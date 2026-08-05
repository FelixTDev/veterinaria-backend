package com.veterinaria.backend.mascota.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoMascotaRequest(@NotNull Boolean activo) {
}
