package com.veterinaria.backend.atencionmedica.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResponse;
import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResumenResponse;
import com.veterinaria.backend.atencionmedica.dto.CrearAtencionMedicaRequest;
import com.veterinaria.backend.atencionmedica.service.AtencionMedicaService;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AtencionMedicaController {
    private final AtencionMedicaService service;
    public AtencionMedicaController(AtencionMedicaService service) { this.service = service; }

    @PostMapping("/citas/{citaId}/atencion-medica")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VETERINARIO')")
    public AtencionMedicaResponse crear(@PathVariable Long citaId, @Valid @RequestBody CrearAtencionMedicaRequest request,
            JwtAuthenticationToken authentication) {
        return service.crear(citaId, currentUserId(authentication), request);
    }

    @GetMapping("/citas/{citaId}/atencion-medica")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VETERINARIO')")
    public AtencionMedicaResponse obtener(@PathVariable Long citaId, JwtAuthenticationToken authentication) {
        return service.obtenerPorCita(citaId, currentUserId(authentication), currentRoles(authentication));
    }

    @GetMapping("/mascotas/{mascotaId}/atenciones-medicas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VETERINARIO')")
    public PaginaResponse<AtencionMedicaResumenResponse> historial(@PathVariable Long mascotaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            JwtAuthenticationToken authentication) {
        return service.historial(mascotaId, currentUserId(authentication), currentRoles(authentication), desde, hasta, page, size);
    }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        Object uid = authentication.getToken().getClaim("uid");
        return uid instanceof Number number ? number.longValue() : Long.valueOf(uid.toString());
    }

    private Set<NombreRol> currentRoles(JwtAuthenticationToken authentication) {
        Set<NombreRol> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                try { roles.add(NombreRol.valueOf(value.substring(5))); } catch (IllegalArgumentException ignored) { }
            }
        }
        return Set.copyOf(roles);
    }
}
