package com.veterinaria.backend.cita.dto;

import com.veterinaria.backend.mascota.enums.EspecieMascota;

public record CitaMascotaResponse(
        Long id,
        String nombre,
        EspecieMascota especie,
        String raza) {
}
