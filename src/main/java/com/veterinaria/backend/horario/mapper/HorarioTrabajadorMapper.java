package com.veterinaria.backend.horario.mapper;

import com.veterinaria.backend.horario.dto.HorarioTrabajadorResponse;
import com.veterinaria.backend.horario.entity.HorarioTrabajador;
import org.springframework.stereotype.Component;

@Component
public class HorarioTrabajadorMapper {
    public HorarioTrabajadorResponse toResponse(HorarioTrabajador horario) {
        return new HorarioTrabajadorResponse(horario.getId(), horario.getUsuario().getId(), horario.getDiaSemana(),
                horario.getHoraInicio(), horario.getHoraFin(), horario.getDescansoInicio(), horario.getDescansoFin(),
                horario.getDisponible(), horario.getCreatedAt(), horario.getUpdatedAt());
    }
}
