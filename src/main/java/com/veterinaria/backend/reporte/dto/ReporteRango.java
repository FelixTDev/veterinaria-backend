package com.veterinaria.backend.reporte.dto;

import java.time.LocalDateTime;

public record ReporteRango(LocalDateTime desde, LocalDateTime hasta) {
}
