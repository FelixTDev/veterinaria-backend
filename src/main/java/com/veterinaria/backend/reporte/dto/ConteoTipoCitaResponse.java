package com.veterinaria.backend.reporte.dto;

import com.veterinaria.backend.cita.enums.TipoCita;

public record ConteoTipoCitaResponse(TipoCita tipo, long cantidad) {
}
