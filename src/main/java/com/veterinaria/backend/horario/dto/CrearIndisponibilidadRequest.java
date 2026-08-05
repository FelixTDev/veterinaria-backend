package com.veterinaria.backend.horario.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearIndisponibilidadRequest(@NotNull @FutureOrPresent LocalDateTime fechaInicio,
        @NotNull LocalDateTime fechaFin, @NotBlank @Size(max = 250) String motivo) {
}
