package com.veterinaria.backend.cita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcarNoAtendidaRequest(
        @NotBlank @Size(max = 500) String motivo) {
}
