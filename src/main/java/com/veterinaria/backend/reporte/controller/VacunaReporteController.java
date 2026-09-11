package com.veterinaria.backend.reporte.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.reporte.dto.VacunasResumenResponse;
import com.veterinaria.backend.reporte.service.VacunaReporteService;

@RestController
@RequestMapping("/api/v1/reportes/vacunas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VETERINARIO')")
public class VacunaReporteController {

    private final VacunaReporteService service;

    public VacunaReporteController(VacunaReporteService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public VacunasResumenResponse resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return service.resumen(desde, hasta);
    }
}
