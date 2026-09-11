package com.veterinaria.backend.reporte.dto;

import java.math.BigDecimal;

import com.veterinaria.backend.pago.enums.MedioPago;

public record IngresoPorMedioResponse(MedioPago medioPago, BigDecimal monto) {
}
