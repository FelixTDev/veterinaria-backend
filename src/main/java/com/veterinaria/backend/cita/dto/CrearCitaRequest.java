package com.veterinaria.backend.cita.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearCitaRequest(
        @NotNull Long mascotaId,
        @NotNull Long trabajadorId,
        @NotNull LocalDateTime fechaHoraInicio,
        @NotEmpty List<@NotNull @Valid ServicioCitaRequest> servicios,
        @Size(max = 500) String motivoConsulta,
        @Size(max = 500) String observaciones) {
}
