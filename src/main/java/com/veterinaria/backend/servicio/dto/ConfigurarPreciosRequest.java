package com.veterinaria.backend.servicio.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ConfigurarPreciosRequest(@NotNull List<@Valid PrecioServicioRequest> precios) {
}
