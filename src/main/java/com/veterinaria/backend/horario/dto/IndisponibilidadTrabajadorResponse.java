package com.veterinaria.backend.horario.dto;

import java.time.LocalDateTime;

public record IndisponibilidadTrabajadorResponse(Long id, Long trabajadorId, LocalDateTime fechaInicio,
        LocalDateTime fechaFin, String motivo, LocalDateTime createdAt) {
}
