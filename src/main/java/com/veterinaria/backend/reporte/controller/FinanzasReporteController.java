package com.veterinaria.backend.reporte.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.reporte.dto.FinanzasResumenResponse;
import com.veterinaria.backend.reporte.dto.SaldoPendienteResponse;
import com.veterinaria.backend.reporte.service.FinanzasReporteService;
import com.veterinaria.backend.usuario.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class FinanzasReporteController {

    private final FinanzasReporteService service;

    public FinanzasReporteController(FinanzasReporteService service) {
        this.service = service;
    }

    @GetMapping("/finanzas/resumen")
    public FinanzasResumenResponse resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return service.resumen(desde, hasta);
    }

    @GetMapping("/pagos/saldos-pendientes")
    public PaginaResponse<SaldoPendienteResponse> saldos(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
        return service.saldosPendientes(desde, hasta, page, size);
    }
}
