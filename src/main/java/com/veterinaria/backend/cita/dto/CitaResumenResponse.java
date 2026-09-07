package com.veterinaria.backend.cita.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;

public record CitaResumenResponse(
        Long id,
        TipoCita tipoCita,
        EstadoCita estado,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        CitaMascotaResponse mascota,
        CitaClienteResponse cliente,
        CitaTrabajadorResponse trabajador,
        List<CitaServicioResponse> servicios) {
}
