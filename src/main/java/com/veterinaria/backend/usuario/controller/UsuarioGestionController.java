package com.veterinaria.backend.usuario.controller;

import java.net.URI;

import com.veterinaria.backend.usuario.dto.ActualizarRolesUsuarioRequest;
import com.veterinaria.backend.usuario.dto.ActualizarUsuarioRequest;
import com.veterinaria.backend.usuario.dto.CambiarEstadoUsuarioRequest;
import com.veterinaria.backend.usuario.dto.CrearUsuarioRequest;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.dto.UsuarioDetalleResponse;
import com.veterinaria.backend.usuario.dto.UsuarioResumenResponse;
import com.veterinaria.backend.usuario.dto.UsuarioRolesResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.service.UsuarioGestionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioGestionController {

    private final UsuarioGestionService usuarioGestionService;

    public UsuarioGestionController(UsuarioGestionService usuarioGestionService) {
        this.usuarioGestionService = usuarioGestionService;
    }

    @PostMapping
    public ResponseEntity<UsuarioDetalleResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioDetalleResponse response = usuarioGestionService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + response.id())).body(response);
    }

    @GetMapping
    public PaginaResponse<UsuarioResumenResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) NombreRol rol,
            @PageableDefault(size = 20) Pageable pageable) {
        return usuarioGestionService.listar(search, activo, rol, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioDetalleResponse obtener(@PathVariable Long id) {
        return usuarioGestionService.obtener(id);
    }

    @PutMapping("/{id}")
    public UsuarioDetalleResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioGestionService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioDetalleResponse cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request,
            JwtAuthenticationToken authentication) {
        return usuarioGestionService.cambiarEstado(id, request, currentUserId(authentication));
    }

    @PutMapping("/{id}/roles")
    public UsuarioRolesResponse actualizarRoles(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRolesUsuarioRequest request,
            JwtAuthenticationToken authentication) {
        return usuarioGestionService.actualizarRoles(id, request, currentUserId(authentication));
    }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        Object uid = authentication.getToken().getClaim("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(uid.toString());
    }
}
