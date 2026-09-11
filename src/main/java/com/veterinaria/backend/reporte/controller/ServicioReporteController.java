package com.veterinaria.backend.reporte.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.reporte.dto.ServicioRankingResponse;
import com.veterinaria.backend.reporte.service.ServicioReporteService;

@RestController
@RequestMapping("/api/v1/reportes/servicios")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class ServicioReporteController {

    private final ServicioReporteService service;

    public ServicioReporteController(ServicioReporteService service) {
        this.service = service;
    }

    @GetMapping("/mas-solicitados")
    public List<ServicioRankingResponse> masSolicitados(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "10") Integer limit) {
        return service.masSolicitados(desde, hasta, limit);
    }
}
