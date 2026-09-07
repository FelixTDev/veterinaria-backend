package com.veterinaria.backend.cita.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record ReprogramarCitaRequest(
        @NotNull Long trabajadorId,
        @NotNull LocalDateTime fechaHoraInicio) {
}
