package com.veterinaria.backend.horario.dto;

import java.time.LocalDateTime;

public record DisponibilidadTrabajadorResponse(Long trabajadorId, LocalDateTime inicio, LocalDateTime fin,
        boolean disponible, String motivo) {
}
