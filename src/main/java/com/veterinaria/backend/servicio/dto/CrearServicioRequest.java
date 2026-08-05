package com.veterinaria.backend.servicio.dto;

import java.math.BigDecimal;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CrearServicioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 250) String descripcion,
        @NotNull TipoServicio tipoServicio,
        @PositiveOrZero BigDecimal precioBase,
        @NotNull @Positive Integer duracionMinutos) {
}
