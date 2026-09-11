package com.veterinaria.backend.reporte.dto;

import com.veterinaria.backend.servicio.enums.TipoServicio;

public record ServicioRankingResponse(Long servicioId, String nombre, TipoServicio tipoServicio, long cantidad) {
}
