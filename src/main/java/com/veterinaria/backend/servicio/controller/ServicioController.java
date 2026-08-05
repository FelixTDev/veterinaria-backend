package com.veterinaria.backend.servicio.controller;

import java.net.URI;
import java.util.List;

import com.veterinaria.backend.servicio.dto.ActualizarServicioRequest;
import com.veterinaria.backend.servicio.dto.CambiarEstadoServicioRequest;
import com.veterinaria.backend.servicio.dto.ConfigurarPreciosRequest;
import com.veterinaria.backend.servicio.dto.CrearServicioRequest;
import com.veterinaria.backend.servicio.dto.PrecioServicioResponse;
import com.veterinaria.backend.servicio.dto.ServicioDetalleResponse;
import com.veterinaria.backend.servicio.dto.ServicioResumenResponse;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.service.ServicioGestionService;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/servicios")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'VETERINARIO', 'PELUQUERO')")
public class ServicioController {

    private final ServicioGestionService service;

    public ServicioController(ServicioGestionService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ServicioDetalleResponse> crear(@Valid @RequestBody CrearServicioRequest request) {
        ServicioDetalleResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/servicios/" + response.id())).body(response);
    }

    @GetMapping
    public PaginaResponse<ServicioResumenResponse> listar(@RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo, @RequestParam(required = false) TipoServicio tipoServicio,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(search, activo, tipoServicio, pageable);
    }

    @GetMapping("/{id}")
    public ServicioDetalleResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ServicioDetalleResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarServicioRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ServicioDetalleResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoServicioRequest request) {
        return service.cambiarEstado(id, request);
    }

    @GetMapping("/{id}/precios")
    public List<PrecioServicioResponse> listarPrecios(@PathVariable Long id) { return service.listarPrecios(id); }

    @PutMapping("/{id}/precios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PrecioServicioResponse> configurarPrecios(@PathVariable Long id,
            @Valid @RequestBody ConfigurarPreciosRequest request) {
        return service.configurarPrecios(id, request);
    }
}
