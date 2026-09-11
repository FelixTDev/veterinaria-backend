package com.veterinaria.backend.reporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.veterinaria.backend.reporte.dto.ReporteRango;

class ReporteRangoValidatorTest {

    @Test
    void acceptsHalfOpenRange() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = desde.plusDays(1);

        assertThat(ReporteRangoValidator.validar(desde, hasta))
                .isEqualTo(new ReporteRango(desde, hasta));
    }

    @Test
    void rejectsNullOrNonIncreasingRange() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThatThrownBy(() -> ReporteRangoValidator.validar(null, desde))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReporteRangoValidator.validar(desde, desde))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReporteRangoValidator.validar(desde, desde.minusMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
