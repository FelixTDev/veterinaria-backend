package com.veterinaria.backend.vacuna.dto;
import java.time.LocalDate;
public record VacunaAplicadaResumenResponse(Long id, Long atencionId, VacunaResumenResponse vacuna, LocalDate fechaAplicacion, LocalDate proximaFecha, String lote, String observaciones) { }
