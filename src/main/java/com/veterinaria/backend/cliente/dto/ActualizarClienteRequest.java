package com.veterinaria.backend.cliente.dto;

import java.time.LocalDate;

import com.veterinaria.backend.cliente.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record ActualizarClienteRequest(
        @NotBlank @Size(max = 60) String primerNombre,
        @Size(max = 60) String segundoNombre,
        @NotBlank @Size(max = 60) String primerApellido,
        @Size(max = 60) String segundoApellido,
        @NotNull TipoDocumento tipoDocumento,
        @NotBlank @Size(max = 20) String numeroDocumento,
        @PastOrPresent LocalDate fechaNacimiento,
        @Size(max = 20) String telefono,
        @Email @Size(max = 150) String correo) {
}
