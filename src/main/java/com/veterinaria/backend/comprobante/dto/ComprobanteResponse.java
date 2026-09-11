package com.veterinaria.backend.comprobante.dto;
import java.time.LocalDateTime;
import com.veterinaria.backend.comprobante.enums.*;
import java.math.BigDecimal;
public record ComprobanteResponse(Long id, Long pagoId, Long citaId, TipoComprobante tipoComprobante,
        String serie, String numero, String ruc, String razonSocial, String direccionFiscal,
        LocalDateTime fechaEmision, EstadoComprobante estado, BigDecimal monto) { }
