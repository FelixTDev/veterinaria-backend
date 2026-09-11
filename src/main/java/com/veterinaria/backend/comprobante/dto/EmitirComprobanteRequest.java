package com.veterinaria.backend.comprobante.dto;
import com.veterinaria.backend.comprobante.enums.TipoComprobante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record EmitirComprobanteRequest(
        @NotNull TipoComprobante tipoComprobante,
        @NotBlank @Size(max=10) String serie,
        @NotBlank @Size(max=20) String numero,
        @Size(max=11) String ruc,
        @Size(max=200) String razonSocial,
        @Size(max=300) String direccionFiscal) { }
