package com.veterinaria.backend.reporte.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.reporte.dto.CitasResumenResponse;
import com.veterinaria.backend.reporte.dto.GranularidadReporte;
import com.veterinaria.backend.reporte.dto.SerieTemporalResponse;
import com.veterinaria.backend.reporte.service.CitaReporteService;

@RestController
@RequestMapping("/api/v1/reportes/citas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'VETERINARIO')")
public class CitaReporteController {

    private final CitaReporteService service;

    public CitaReporteController(CitaReporteService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public CitasResumenResponse resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return service.resumen(desde, hasta);
    }

    @GetMapping("/tendencia")
    public List<SerieTemporalResponse> tendencia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam GranularidadReporte granularidad) {
        return service.tendencia(desde, hasta, granularidad);
    }
}
