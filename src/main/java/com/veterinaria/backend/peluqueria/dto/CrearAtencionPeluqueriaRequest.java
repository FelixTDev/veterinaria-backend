package com.veterinaria.backend.peluqueria.dto;

import jakarta.validation.constraints.Size;

public record CrearAtencionPeluqueriaRequest(
        @Size(max = 500) String observaciones) {
}
