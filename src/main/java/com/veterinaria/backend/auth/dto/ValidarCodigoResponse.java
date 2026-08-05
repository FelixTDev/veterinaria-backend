package com.veterinaria.backend.auth.dto;

public record ValidarCodigoResponse(
        String recoveryToken,
        String tokenType,
        long expiresIn) {
}
