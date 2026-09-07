package com.veterinaria.backend.cita.dto;

import java.math.BigDecimal;

import com.veterinaria.backend.servicio.enums.TipoServicio;

public record CitaServicioResponse(
        Long id,
        String nombre,
        TipoServicio tipoServicio,
        Long precioServicioTamanoId,
        BigDecimal precioAplicado,
        Integer duracionAplicadaMinutos) {
}
