package com.veterinaria.backend.vacuna.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CrearVacunaRequest(@NotBlank @Size(max = 120) String nombre, @Size(max = 250) String descripcion) { }
