package com.veterinaria.backend.servicio.dto;

import com.veterinaria.backend.servicio.enums.TipoServicio;

public record ServicioResumenResponse(Long id, String nombre, TipoServicio tipoServicio,
        Integer duracionMinutos, Boolean activo, boolean preciosConfigurados) {
}
