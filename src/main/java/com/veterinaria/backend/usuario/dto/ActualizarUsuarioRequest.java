package com.veterinaria.backend.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 60) String primerNombre,
        @Size(max = 60) String segundoNombre,
        @NotBlank @Size(max = 60) String primerApellido,
        @Size(max = 60) String segundoApellido,
        @NotBlank @Email @Size(max = 150) String correo,
        @Size(max = 20) String telefono) {

    public ActualizarUsuarioRequest {
        if (correo != null) {
            correo = correo.trim();
        }
    }
}
