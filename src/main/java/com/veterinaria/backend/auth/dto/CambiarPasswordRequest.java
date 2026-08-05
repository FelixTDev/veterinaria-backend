package com.veterinaria.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarPasswordRequest(
        @NotBlank String passwordActual,
        @NotBlank String passwordNueva,
        @NotBlank String confirmacionPassword) {
}
