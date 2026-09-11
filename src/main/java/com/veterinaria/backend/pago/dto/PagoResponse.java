package com.veterinaria.backend.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.veterinaria.backend.pago.enums.EstadoPago;

public record PagoResponse(Long id, Long citaId, Long registradoPorId, BigDecimal montoTotal,
        EstadoPago estado, LocalDateTime fechaPago, String observaciones, List<DetallePagoResponse> detalles) { }
