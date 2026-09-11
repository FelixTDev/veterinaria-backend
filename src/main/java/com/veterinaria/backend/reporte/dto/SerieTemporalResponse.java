package com.veterinaria.backend.reporte.dto;

import java.time.LocalDateTime;

public record SerieTemporalResponse(LocalDateTime periodo, long cantidad) {
}
