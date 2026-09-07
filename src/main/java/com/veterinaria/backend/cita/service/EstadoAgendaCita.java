package com.veterinaria.backend.cita.service;

import java.util.Set;

import com.veterinaria.backend.cita.enums.EstadoCita;

public final class EstadoAgendaCita {

    private static final Set<EstadoCita> OCUPANTES = Set.of(
            EstadoCita.PENDIENTE,
            EstadoCita.CONFIRMADA);

    private EstadoAgendaCita() {
    }

    public static Set<EstadoCita> ocupantes() {
        return OCUPANTES;
    }
}
