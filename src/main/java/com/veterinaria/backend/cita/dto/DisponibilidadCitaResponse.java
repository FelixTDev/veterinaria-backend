package com.veterinaria.backend.cita.dto;

import java.time.LocalDateTime;

import com.veterinaria.backend.cita.enums.TipoCita;

public record DisponibilidadCitaResponse(
        Long trabajadorId,
        TipoCita tipoCita,
        LocalDateTime inicio,
        LocalDateTime fin,
        boolean disponibilidadBase,
        boolean tieneCitaSolapada,
        boolean disponible,
        String motivo) {
}
