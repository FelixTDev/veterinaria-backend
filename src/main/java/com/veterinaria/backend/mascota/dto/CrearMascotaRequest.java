package com.veterinaria.backend.mascota.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CrearMascotaRequest(
        @NotNull Long clienteId,
        @NotBlank @Size(max = 80) String nombre,
        @NotNull EspecieMascota especie,
        @Size(max = 80) String raza,
        @Size(max = 80) String color,
        SexoMascota sexo,
        @Positive BigDecimal pesoKg,
        @PastOrPresent LocalDate fechaNacimiento,
        @PositiveOrZero Integer edadAproximadaAnios) {
}
