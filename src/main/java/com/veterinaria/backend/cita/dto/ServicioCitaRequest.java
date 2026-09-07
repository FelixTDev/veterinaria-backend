package com.veterinaria.backend.cita.dto;

import jakarta.validation.constraints.NotNull;

public record ServicioCitaRequest(
        @NotNull Long servicioId,
        Long precioServicioTamanoId) {
}
