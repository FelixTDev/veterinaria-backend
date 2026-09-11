package com.veterinaria.backend.pago.dto;

import java.math.BigDecimal;
import com.veterinaria.backend.pago.enums.MedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DetallePagoRequest(
        @NotNull MedioPago medioPago,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @Size(max = 100) String referencia) { }
