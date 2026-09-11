package com.veterinaria.backend.reporte.dto;

import java.util.List;

public record VacunasResumenResponse(long totalAplicadas, List<VacunaRankingResponse> masAplicadas) {
}
