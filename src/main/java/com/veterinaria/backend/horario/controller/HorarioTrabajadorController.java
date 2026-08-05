package com.veterinaria.backend.horario.controller;

import java.net.URI;
import java.util.List;

import com.veterinaria.backend.horario.dto.ActualizarHorarioRequest;
import com.veterinaria.backend.horario.dto.CambiarDisponibilidadRequest;
import com.veterinaria.backend.horario.dto.CrearHorarioRequest;
import com.veterinaria.backend.horario.dto.HorarioTrabajadorResponse;
import com.veterinaria.backend.horario.service.HorarioTrabajadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trabajadores/{trabajadorId}/horarios")
public class HorarioTrabajadorController {

    private final HorarioTrabajadorService service;

    public HorarioTrabajadorController(HorarioTrabajadorService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<HorarioTrabajadorResponse> crear(@PathVariable Long trabajadorId,
            @Valid @RequestBody CrearHorarioRequest request) {
        HorarioTrabajadorResponse response = service.crear(trabajadorId, request);
        return ResponseEntity.created(URI.create("/api/v1/trabajadores/" + trabajadorId + "/horarios/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA') or (hasAnyRole('VETERINARIO', 'PELUQUERO') and @securityAccess.isCurrentUser(#trabajadorId, authentication))")
    public List<HorarioTrabajadorResponse> listar(@PathVariable Long trabajadorId) { return service.listar(trabajadorId); }

    @GetMapping("/{horarioId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA') or (hasAnyRole('VETERINARIO', 'PELUQUERO') and @securityAccess.isCurrentUser(#trabajadorId, authentication))")
    public HorarioTrabajadorResponse obtener(@PathVariable Long trabajadorId, @PathVariable Long horarioId) {
        return service.obtener(trabajadorId, horarioId);
    }

    @PutMapping("/{horarioId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public HorarioTrabajadorResponse actualizar(@PathVariable Long trabajadorId, @PathVariable Long horarioId,
            @Valid @RequestBody ActualizarHorarioRequest request) {
        return service.actualizar(trabajadorId, horarioId, request);
    }

    @PatchMapping("/{horarioId}/disponibilidad")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public HorarioTrabajadorResponse cambiarDisponibilidad(@PathVariable Long trabajadorId, @PathVariable Long horarioId,
            @Valid @RequestBody CambiarDisponibilidadRequest request) {
        return service.cambiarDisponibilidad(trabajadorId, horarioId, request);
    }
}
