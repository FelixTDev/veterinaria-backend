package com.veterinaria.backend.reporte.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanzasResumenResponse(BigDecimal ingresoTotal, long cantidadPagos,
        List<IngresoPorMedioResponse> ingresoPorMedio) {
}
