package com.veterinaria.backend.vacuna.dto;
import java.time.LocalDateTime;
public record VacunaResponse(Long id, String nombre, String descripcion, Boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt) { }
