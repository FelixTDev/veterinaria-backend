package com.veterinaria.backend.cliente.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoClienteRequest(@NotNull Boolean activo) {
}
