package com.veterinaria.backend.reporte.dto;

import java.util.List;

public record CitasResumenResponse(long total, List<ConteoEstadoCitaResponse> porEstado,
        List<ConteoTipoCitaResponse> porTipo) {
}
