package com.veterinaria.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RestablecerPasswordRequest(
        @NotBlank String recoveryToken,
        @NotBlank String passwordNueva,
        @NotBlank String confirmacionPassword) {
}
