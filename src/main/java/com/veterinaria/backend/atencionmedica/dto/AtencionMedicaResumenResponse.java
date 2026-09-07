package com.veterinaria.backend.atencionmedica.dto;

import java.time.LocalDateTime;

public record AtencionMedicaResumenResponse(
        Long id,
        Long citaId,
        LocalDateTime fechaAtencion,
        VeterinarioResumenResponse veterinario,
        String diagnosticoResumen) {
}
