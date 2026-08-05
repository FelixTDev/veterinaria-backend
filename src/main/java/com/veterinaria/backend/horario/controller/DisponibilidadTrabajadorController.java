package com.veterinaria.backend.horario.controller;

import java.time.LocalDateTime;

import com.veterinaria.backend.horario.dto.DisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.service.DisponibilidadTrabajadorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trabajadores/{trabajadorId}/disponibilidad")
public class DisponibilidadTrabajadorController {

    private final DisponibilidadTrabajadorService service;

    public DisponibilidadTrabajadorController(DisponibilidadTrabajadorService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA') or (hasAnyRole('VETERINARIO', 'PELUQUERO') and @securityAccess.isCurrentUser(#trabajadorId, authentication))")
    public DisponibilidadTrabajadorResponse consultar(@PathVariable Long trabajadorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return service.consultar(trabajadorId, inicio, fin);
    }
}
