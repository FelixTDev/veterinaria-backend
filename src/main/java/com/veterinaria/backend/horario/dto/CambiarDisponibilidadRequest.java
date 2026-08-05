package com.veterinaria.backend.horario.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarDisponibilidadRequest(@NotNull Boolean disponible) {
}
