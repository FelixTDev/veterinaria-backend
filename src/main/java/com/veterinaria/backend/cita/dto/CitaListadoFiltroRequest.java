package com.veterinaria.backend.cita.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;

import org.springframework.format.annotation.DateTimeFormat;

public record CitaListadoFiltroRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime fechaHoraInicioDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime fechaHoraInicioHasta,
        EstadoCita estado,
        TipoCita tipoCita,
        Long clienteId,
        Long mascotaId,
        Long trabajadorId,
        String busqueda,
        Integer page,
        Integer size,
        List<String> sort) {
}
