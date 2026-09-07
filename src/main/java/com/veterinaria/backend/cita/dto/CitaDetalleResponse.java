package com.veterinaria.backend.cita.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;

public record CitaDetalleResponse(
        Long id,
        TipoCita tipoCita,
        EstadoCita estado,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String motivoConsulta,
        String motivoNoAtencion,
        String motivoCancelacion,
        String observaciones,
        CitaMascotaResponse mascota,
        CitaClienteResponse cliente,
        CitaTrabajadorResponse trabajador,
        CitaUsuarioResponse registradoPor,
        List<CitaServicioResponse> servicios,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
