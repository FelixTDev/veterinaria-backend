package com.veterinaria.backend.reporte.service;

import java.time.LocalDateTime;

import com.veterinaria.backend.reporte.dto.ReporteRango;

public final class ReporteRangoValidator {

    private ReporteRangoValidator() {
    }

    public static ReporteRango validar(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new IllegalArgumentException("El rango debe cumplir desde < hasta.");
        }
        return new ReporteRango(desde, hasta);
    }
}
