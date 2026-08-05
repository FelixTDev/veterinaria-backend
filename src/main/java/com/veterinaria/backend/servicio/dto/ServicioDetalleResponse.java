package com.veterinaria.backend.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.veterinaria.backend.servicio.enums.TipoServicio;

public record ServicioDetalleResponse(Long id, String nombre, String descripcion, TipoServicio tipoServicio,
        BigDecimal precioBase, Integer duracionMinutos, Boolean activo, LocalDateTime createdAt,
        LocalDateTime updatedAt, List<PrecioServicioResponse> precios) {
}
