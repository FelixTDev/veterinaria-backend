package com.veterinaria.backend.vacuna.dto;
import jakarta.validation.constraints.NotNull;
public record CambiarEstadoVacunaRequest(@NotNull Boolean activo) { }
