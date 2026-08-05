package com.veterinaria.backend.mascota.dto;

import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;

public record MascotaResumenResponse(
        Long id,
        Long clienteId,
        String propietario,
        String nombre,
        EspecieMascota especie,
        String raza,
        SexoMascota sexo,
        Boolean activo) {
}
