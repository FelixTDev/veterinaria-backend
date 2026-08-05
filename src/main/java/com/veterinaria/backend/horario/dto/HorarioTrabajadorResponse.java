package com.veterinaria.backend.horario.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record HorarioTrabajadorResponse(Long id, Long trabajadorId, Integer diaSemana, LocalTime horaInicio,
        LocalTime horaFin, LocalTime descansoInicio, LocalTime descansoFin, Boolean disponible,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
