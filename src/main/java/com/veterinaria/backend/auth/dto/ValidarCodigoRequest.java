package com.veterinaria.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidarCodigoRequest(
        @NotBlank @Email String correo,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "El codigo debe tener exactamente 6 digitos") String codigo) {
}
