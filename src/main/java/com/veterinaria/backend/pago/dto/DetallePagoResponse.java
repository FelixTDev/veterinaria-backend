package com.veterinaria.backend.pago.dto;

import java.math.BigDecimal;
import com.veterinaria.backend.pago.enums.MedioPago;

public record DetallePagoResponse(Long id, MedioPago medioPago, BigDecimal monto, String referencia) { }
