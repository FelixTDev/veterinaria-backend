package com.veterinaria.backend.servicio.dto;

import java.math.BigDecimal;
import com.veterinaria.backend.servicio.enums.TamanoMascota;

public record PrecioServicioResponse(Long id, TamanoMascota tamanoMascota, BigDecimal precio,
        Integer duracionMinutos, Boolean activo) {
}
