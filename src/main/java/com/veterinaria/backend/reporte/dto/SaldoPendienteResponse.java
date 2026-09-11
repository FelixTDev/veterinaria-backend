package com.veterinaria.backend.reporte.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaldoPendienteResponse(Long citaId, LocalDateTime fechaHoraInicio, Long mascotaId,
        String mascota, Long clienteId, String cliente, BigDecimal totalServicios,
        BigDecimal totalPagado, BigDecimal saldoPendiente) {
}
