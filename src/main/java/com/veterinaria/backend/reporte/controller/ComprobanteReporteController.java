package com.veterinaria.backend.reporte.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.reporte.dto.ComprobantesResumenResponse;
import com.veterinaria.backend.reporte.service.ComprobanteReporteService;

@RestController
@RequestMapping("/api/v1/reportes/comprobantes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class ComprobanteReporteController {

    private final ComprobanteReporteService service;

    public ComprobanteReporteController(ComprobanteReporteService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public ComprobantesResumenResponse resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return service.resumen(desde, hasta);
    }
}
