package com.veterinaria.backend.vacuna.dto;
import java.time.LocalDate;
public record VacunaAplicadaResponse(Long id, Long atencionId, Long mascotaId, VacunaResumenResponse vacuna, LocalDate fechaAplicacion, LocalDate proximaFecha, String lote, String observaciones) { }
