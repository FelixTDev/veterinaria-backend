package com.veterinaria.backend.servicio.dto;

import java.math.BigDecimal;
import com.veterinaria.backend.servicio.enums.TamanoMascota;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PrecioServicioRequest(
        @NotNull TamanoMascota tamanoMascota,
        @NotNull @PositiveOrZero BigDecimal precio,
        @NotNull @Positive Integer duracionMinutos) {
}
