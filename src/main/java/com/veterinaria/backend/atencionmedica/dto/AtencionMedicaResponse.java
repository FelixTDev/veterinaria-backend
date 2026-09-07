package com.veterinaria.backend.atencionmedica.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtencionMedicaResponse(
        Long id,
        Long citaId,
        Long mascotaId,
        String mascotaNombre,
        VeterinarioResumenResponse veterinario,
        BigDecimal pesoKg,
        BigDecimal temperaturaC,
        String sintomas,
        String diagnostico,
        String motivoSinDiagnostico,
        String tratamiento,
        String motivoSinTratamiento,
        String receta,
        String motivoSinReceta,
        String observaciones,
        LocalDateTime fechaAtencion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
