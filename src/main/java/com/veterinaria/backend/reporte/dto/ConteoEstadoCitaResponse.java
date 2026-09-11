package com.veterinaria.backend.reporte.dto;

import com.veterinaria.backend.cita.enums.EstadoCita;

public record ConteoEstadoCitaResponse(EstadoCita estado, long cantidad) {
}
