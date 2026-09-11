package com.veterinaria.backend.reporte.dto;

import java.math.BigDecimal;

public record DashboardResumenResponse(CitasResumenResponse citas, long atencionesMedicas,
        long atencionesPeluqueria, long clientesActivos, long mascotasActivas,
        long serviciosActivos, BigDecimal ingresosCobrados, BigDecimal saldoPendiente,
        long comprobantesEmitidos) {
}
