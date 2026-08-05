package com.veterinaria.backend.horario.dto;

import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;

public record CrearHorarioRequest(@NotNull Integer diaSemana, @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin, LocalTime descansoInicio, LocalTime descansoFin) {
}
