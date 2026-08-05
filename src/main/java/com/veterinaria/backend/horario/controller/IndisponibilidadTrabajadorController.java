package com.veterinaria.backend.horario.controller;

import java.net.URI;
import java.util.List;

import com.veterinaria.backend.horario.dto.ActualizarIndisponibilidadRequest;
import com.veterinaria.backend.horario.dto.CrearIndisponibilidadRequest;
import com.veterinaria.backend.horario.dto.IndisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.service.IndisponibilidadTrabajadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trabajadores/{trabajadorId}/indisponibilidades")
public class IndisponibilidadTrabajadorController {

    private final IndisponibilidadTrabajadorService service;

    public IndisponibilidadTrabajadorController(IndisponibilidadTrabajadorService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<IndisponibilidadTrabajadorResponse> crear(@PathVariable Long trabajadorId,
            @Valid @RequestBody CrearIndisponibilidadRequest request) {
        IndisponibilidadTrabajadorResponse response = service.crear(trabajadorId, request);
        return ResponseEntity.created(URI.create("/api/v1/trabajadores/" + trabajadorId + "/indisponibilidades/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA') or (hasAnyRole('VETERINARIO', 'PELUQUERO') and @securityAccess.isCurrentUser(#trabajadorId, authentication))")
    public List<IndisponibilidadTrabajadorResponse> listar(@PathVariable Long trabajadorId) { return service.listar(trabajadorId); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA') or (hasAnyRole('VETERINARIO', 'PELUQUERO') and @securityAccess.isCurrentUser(#trabajadorId, authentication))")
    public IndisponibilidadTrabajadorResponse obtener(@PathVariable Long trabajadorId, @PathVariable Long id) {
        return service.obtener(trabajadorId, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public IndisponibilidadTrabajadorResponse actualizar(@PathVariable Long trabajadorId, @PathVariable Long id,
            @Valid @RequestBody ActualizarIndisponibilidadRequest request) {
        return service.actualizar(trabajadorId, id, request);
    }
}
