package com.veterinaria.backend.vacuna.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record AplicarVacunaRequest(@NotNull Long vacunaId, LocalDate proximaFecha, @Size(max = 80) String lote, @Size(max = 300) String observaciones) { }
