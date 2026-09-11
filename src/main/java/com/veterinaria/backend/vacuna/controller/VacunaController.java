package com.veterinaria.backend.vacuna.controller;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.vacuna.dto.*;
import com.veterinaria.backend.vacuna.service.VacunaAplicadaService;
import com.veterinaria.backend.vacuna.service.VacunaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class VacunaController {
    private final VacunaService vacunaService;
    private final VacunaAplicadaService aplicadaService;

    public VacunaController(VacunaService vacunaService, VacunaAplicadaService aplicadaService) {
        this.vacunaService = vacunaService; this.aplicadaService = aplicadaService;
    }

    @PostMapping("/vacunas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public VacunaResponse crear(@Valid @RequestBody CrearVacunaRequest request) { return vacunaService.crear(request); }

    @GetMapping("/vacunas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    public PaginaResponse<VacunaResumenResponse> listar(@RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) { return vacunaService.listar(activo, page, size); }

    @GetMapping("/vacunas/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    public VacunaResponse obtener(@PathVariable Long id) { return vacunaService.obtener(id); }

    @PutMapping("/vacunas/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public VacunaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarVacunaRequest request) { return vacunaService.actualizar(id, request); }

    @PatchMapping("/vacunas/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public VacunaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoVacunaRequest request) { return vacunaService.cambiarEstado(id, request); }

    @PostMapping("/atenciones-medicas/{atencionId}/vacunas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    public VacunaAplicadaResponse aplicar(@PathVariable Long atencionId, @Valid @RequestBody AplicarVacunaRequest request, JwtAuthenticationToken authentication) { return aplicadaService.aplicar(atencionId, currentUserId(authentication), request); }

    @GetMapping("/atenciones-medicas/{atencionId}/vacunas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    public List<VacunaAplicadaResponse> listarPorAtencion(@PathVariable Long atencionId) { return aplicadaService.listarPorAtencion(atencionId); }

    @GetMapping("/mascotas/{mascotaId}/vacunas-aplicadas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    public PaginaResponse<VacunaAplicadaResumenResponse> historial(@PathVariable Long mascotaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long vacunaId, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) { return aplicadaService.historial(mascotaId, desde, hasta, vacunaId, page, size); }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        Object uid = authentication.getToken().getClaim("uid");
        return uid instanceof Number number ? number.longValue() : Long.valueOf(uid.toString());
    }

    @SuppressWarnings("unused")
    private Set<NombreRol> currentRoles(JwtAuthenticationToken authentication) {
        Set<NombreRol> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) try { roles.add(NombreRol.valueOf(value.substring(5))); } catch (IllegalArgumentException ignored) { }
        }
        return Set.copyOf(roles);
    }
}
