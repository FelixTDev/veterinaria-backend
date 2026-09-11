package com.veterinaria.backend.reporte.projection;

import java.time.LocalDateTime;

public interface SerieTemporalProjection {
    LocalDateTime getPeriodo();
    long getCantidad();
}
