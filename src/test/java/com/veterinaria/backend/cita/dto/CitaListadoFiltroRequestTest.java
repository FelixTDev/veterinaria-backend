package com.veterinaria.backend.cita.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;

class CitaListadoFiltroRequestTest {

    @Test
    void shouldAnnotateFechaHoraFiltersWithIsoDateTimeForModelAttributeBinding() throws Exception {
        var fechaHoraInicioDesdeField = CitaListadoFiltroRequest.class.getDeclaredField("fechaHoraInicioDesde");
        var fechaHoraInicioHastaField = CitaListadoFiltroRequest.class.getDeclaredField("fechaHoraInicioHasta");
        var fechaHoraInicioDesdeAccessor = CitaListadoFiltroRequest.class.getDeclaredMethod("fechaHoraInicioDesde");
        var fechaHoraInicioHastaAccessor = CitaListadoFiltroRequest.class.getDeclaredMethod("fechaHoraInicioHasta");

        assertThat(fechaHoraInicioDesdeField.getType()).isEqualTo(LocalDateTime.class);
        assertThat(fechaHoraInicioHastaField.getType()).isEqualTo(LocalDateTime.class);
        assertThat(fechaHoraInicioDesdeField.getAnnotation(DateTimeFormat.class)).isNotNull();
        assertThat(fechaHoraInicioHastaField.getAnnotation(DateTimeFormat.class)).isNotNull();
        assertThat(fechaHoraInicioDesdeAccessor.getAnnotation(DateTimeFormat.class)).isNotNull();
        assertThat(fechaHoraInicioHastaAccessor.getAnnotation(DateTimeFormat.class)).isNotNull();
        assertThat(fechaHoraInicioDesdeField.getAnnotation(DateTimeFormat.class).iso()).isEqualTo(DateTimeFormat.ISO.DATE_TIME);
        assertThat(fechaHoraInicioHastaField.getAnnotation(DateTimeFormat.class).iso()).isEqualTo(DateTimeFormat.ISO.DATE_TIME);
    }
}
