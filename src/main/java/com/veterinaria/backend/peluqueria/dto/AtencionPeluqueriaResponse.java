package com.veterinaria.backend.peluqueria.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.cita.enums.EstadoCita;

public record AtencionPeluqueriaResponse(
        Long id,
        Long citaId,
        Long peluqueroId,
        String observaciones,
        LocalDateTime fechaAtencion,
        EstadoCita estadoCita,
        List<FotoPeluqueriaResponse> evidencias) {
}
