package com.veterinaria.backend.mascota.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;

public record MascotaDetalleResponse(
        Long id,
        Long clienteId,
        String propietario,
        String nombre,
        EspecieMascota especie,
        String raza,
        String color,
        SexoMascota sexo,
        BigDecimal pesoKg,
        LocalDate fechaNacimiento,
        Integer edadAproximadaAnios,
        Boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
