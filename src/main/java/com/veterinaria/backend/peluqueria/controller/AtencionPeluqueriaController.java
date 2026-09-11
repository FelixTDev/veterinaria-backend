package com.veterinaria.backend.peluqueria.controller;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.veterinaria.backend.peluqueria.dto.AtencionPeluqueriaResponse;
import com.veterinaria.backend.peluqueria.dto.CrearAtencionPeluqueriaRequest;
import com.veterinaria.backend.peluqueria.dto.FotoPeluqueriaResponse;
import com.veterinaria.backend.peluqueria.enums.TipoFoto;
import com.veterinaria.backend.peluqueria.service.AtencionPeluqueriaService;
import com.veterinaria.backend.usuario.enums.NombreRol;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AtencionPeluqueriaController {

    private final AtencionPeluqueriaService service;

    public AtencionPeluqueriaController(AtencionPeluqueriaService service) {
        this.service = service;
    }

    @PostMapping("/citas/{citaId}/atencion-peluqueria")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PELUQUERO')")
    public AtencionPeluqueriaResponse crear(
            @PathVariable Long citaId,
            @Valid @RequestBody(required = false) CrearAtencionPeluqueriaRequest request,
            JwtAuthenticationToken authentication) {
        return service.crear(citaId, currentUserId(authentication), currentRoles(authentication),
                request == null ? null : request.observaciones());
    }

    @GetMapping("/citas/{citaId}/atencion-peluqueria")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PELUQUERO')")
    public AtencionPeluqueriaResponse obtener(
            @PathVariable Long citaId,
            JwtAuthenticationToken authentication) {
        return service.obtenerPorCita(citaId, currentUserId(authentication), currentRoles(authentication));
    }

    @PostMapping(value = "/atenciones-peluqueria/{id}/evidencias", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PELUQUERO')")
    public FotoPeluqueriaResponse agregarEvidencia(
            @PathVariable Long id,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam TipoFoto tipoFoto,
            JwtAuthenticationToken authentication) {
        return service.agregarEvidencia(id, currentUserId(authentication), currentRoles(authentication), archivo, tipoFoto);
    }

    @GetMapping("/atenciones-peluqueria/{id}/evidencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PELUQUERO')")
    public List<FotoPeluqueriaResponse> listarEvidencias(
            @PathVariable Long id,
            JwtAuthenticationToken authentication) {
        return service.listarEvidencias(id, currentUserId(authentication), currentRoles(authentication));
    }

    @PatchMapping("/atenciones-peluqueria/{id}/cerrar")
    @PreAuthorize("hasRole('PELUQUERO')")
    public AtencionPeluqueriaResponse cerrar(
            @PathVariable Long id,
            JwtAuthenticationToken authentication) {
        return service.cerrar(id, currentUserId(authentication), currentRoles(authentication));
    }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        if (authentication == null) throw new AuthenticationCredentialsNotFoundException("Token sin uid valido.");
        Object uid = authentication.getToken().getClaim("uid");
        try {
            if (uid instanceof Number number) return number.longValue();
            if (uid != null) return Long.valueOf(uid.toString());
        } catch (NumberFormatException ignored) {
            // Se traduce uniformemente a 401.
        }
        throw new AuthenticationCredentialsNotFoundException("Token sin uid valido.");
    }

    private Set<NombreRol> currentRoles(JwtAuthenticationToken authentication) {
        Set<NombreRol> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                try { roles.add(NombreRol.valueOf(value.substring(5))); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        if (roles.isEmpty()) throw new AccessDeniedException("Token sin roles operativos.");
        return Set.copyOf(roles);
    }
}
