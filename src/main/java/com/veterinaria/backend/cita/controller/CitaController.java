package com.veterinaria.backend.cita.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.backend.cita.dto.CancelarCitaRequest;
import com.veterinaria.backend.cita.dto.CitaDetalleResponse;
import com.veterinaria.backend.cita.dto.CitaListadoFiltroRequest;
import com.veterinaria.backend.cita.dto.CitaResumenResponse;
import com.veterinaria.backend.cita.dto.CrearCitaRequest;
import com.veterinaria.backend.cita.dto.DisponibilidadCitaResponse;
import com.veterinaria.backend.cita.dto.MarcarNoAtendidaRequest;
import com.veterinaria.backend.cita.dto.ReprogramarCitaRequest;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.service.CitaGestionService;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {

    private static final String ROLES_OPERATIVOS =
            "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'VETERINARIO', 'PELUQUERO')";

    private final CitaGestionService citaGestionService;

    public CitaController(CitaGestionService citaGestionService) {
        this.citaGestionService = citaGestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public CitaDetalleResponse crear(
            @Valid @RequestBody CrearCitaRequest request,
            JwtAuthenticationToken authentication) {
        return citaGestionService.crear(request, currentUserId(authentication));
    }

    @GetMapping
    @PreAuthorize(ROLES_OPERATIVOS)
    public PaginaResponse<CitaResumenResponse> listar(
            @ModelAttribute CitaListadoFiltroRequest filtro,
            JwtAuthenticationToken authentication) {
        return citaGestionService.listar(filtro, currentUserId(authentication), currentRoles(authentication));
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize(ROLES_OPERATIVOS)
    public DisponibilidadCitaResponse consultarDisponibilidad(
            @RequestParam Long trabajadorId,
            @RequestParam TipoCita tipoCita,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            JwtAuthenticationToken authentication) {
        return citaGestionService.consultarDisponibilidad(
                trabajadorId,
                tipoCita,
                inicio,
                fin,
                currentUserId(authentication),
                currentRoles(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ROLES_OPERATIVOS)
    public CitaDetalleResponse obtener(
            @PathVariable Long id,
            JwtAuthenticationToken authentication) {
        return citaGestionService.obtener(id, currentUserId(authentication), currentRoles(authentication));
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize(ROLES_OPERATIVOS)
    public CitaDetalleResponse confirmar(
            @PathVariable Long id,
            JwtAuthenticationToken authentication) {
        return citaGestionService.confirmar(id, currentUserId(authentication), currentRoles(authentication));
    }

    @PatchMapping("/{id}/reprogramar")
    @PreAuthorize(ROLES_OPERATIVOS)
    public CitaDetalleResponse reprogramar(
            @PathVariable Long id,
            @Valid @RequestBody ReprogramarCitaRequest request,
            JwtAuthenticationToken authentication) {
        return citaGestionService.reprogramar(
                id,
                request,
                currentUserId(authentication),
                currentRoles(authentication));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize(ROLES_OPERATIVOS)
    public CitaDetalleResponse cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarCitaRequest request,
            JwtAuthenticationToken authentication) {
        return citaGestionService.cancelar(
                id,
                request,
                currentUserId(authentication),
                currentRoles(authentication));
    }

    @PatchMapping("/{id}/no-atendida")
    @PreAuthorize(ROLES_OPERATIVOS)
    public CitaDetalleResponse marcarNoAtendida(
            @PathVariable Long id,
            @Valid @RequestBody MarcarNoAtendidaRequest request,
            JwtAuthenticationToken authentication) {
        return citaGestionService.marcarNoAtendida(
                id,
                request,
                currentUserId(authentication),
                currentRoles(authentication));
    }

    @PatchMapping("/{id}/atendida")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VETERINARIO', 'PELUQUERO')")
    public CitaDetalleResponse marcarAtendida(
            @PathVariable Long id,
            JwtAuthenticationToken authentication) {
        return citaGestionService.marcarAtendida(
                id,
                currentUserId(authentication),
                currentRoles(authentication));
    }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("Token sin uid valido.");
        }
        Object uid = authentication.getToken().getClaim("uid");
        try {
            if (uid instanceof Number number) {
                return number.longValue();
            }
            if (uid != null) {
                return Long.valueOf(uid.toString());
            }
        } catch (NumberFormatException ignored) {
            // Se traduce de forma uniforme a 403; nunca se usa un identificador aportado por el request.
        }
        throw new AuthenticationCredentialsNotFoundException("Token sin uid valido.");
    }

    private Set<NombreRol> currentRoles(JwtAuthenticationToken authentication) {
        Set<NombreRol> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value == null || !value.startsWith("ROLE_")) {
                continue;
            }
            try {
                roles.add(NombreRol.valueOf(value.substring("ROLE_".length())));
            } catch (IllegalArgumentException ignored) {
                // Una autoridad ajena al dominio no amplía permisos.
            }
        }
        if (roles.isEmpty()) {
            throw new AccessDeniedException("Token sin roles operativos.");
        }
        return Set.copyOf(roles);
    }
}
