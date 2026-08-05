package com.veterinaria.backend.horario.mapper;

import com.veterinaria.backend.horario.dto.IndisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.entity.IndisponibilidadTrabajador;
import org.springframework.stereotype.Component;

@Component
public class IndisponibilidadTrabajadorMapper {
    public IndisponibilidadTrabajadorResponse toResponse(IndisponibilidadTrabajador value) {
        return new IndisponibilidadTrabajadorResponse(value.getId(), value.getUsuario().getId(), value.getFechaInicio(),
                value.getFechaFin(), value.getMotivo(), value.getCreatedAt());
    }
}
