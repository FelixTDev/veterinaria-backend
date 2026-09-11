package com.veterinaria.backend.pago.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RegistrarPagoRequest(
        @NotEmpty @Valid List<DetallePagoRequest> detalles,
        @Size(max = 300) String observaciones) { }
