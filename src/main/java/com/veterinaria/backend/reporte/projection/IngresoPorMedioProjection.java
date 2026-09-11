package com.veterinaria.backend.reporte.projection;

import java.math.BigDecimal;

public interface IngresoPorMedioProjection {
    String getMedioPago();
    BigDecimal getMonto();
}
