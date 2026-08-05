package com.veterinaria.backend.cliente.controller;

import java.net.URI;

import com.veterinaria.backend.cliente.dto.ActualizarClienteRequest;
import com.veterinaria.backend.cliente.dto.CambiarEstadoClienteRequest;
import com.veterinaria.backend.cliente.dto.ClienteDetalleResponse;
import com.veterinaria.backend.cliente.dto.ClienteResumenResponse;
import com.veterinaria.backend.cliente.dto.CrearClienteRequest;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.service.ClienteGestionService;
import com.veterinaria.backend.mascota.dto.MascotaResumenResponse;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
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
@RequestMapping("/api/v1/clientes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class ClienteController {

    private final ClienteGestionService service;

    public ClienteController(ClienteGestionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ClienteDetalleResponse> crear(@Valid @RequestBody CrearClienteRequest request) {
        ClienteDetalleResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + response.id())).body(response);
    }

    @GetMapping
    public PaginaResponse<ClienteResumenResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(search, activo, tipoDocumento, pageable);
    }

    @GetMapping("/{id}")
    public ClienteDetalleResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PutMapping("/{id}")
    public ClienteDetalleResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarClienteRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ClienteDetalleResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoClienteRequest request) {
        return service.cambiarEstado(id, request);
    }

    @GetMapping("/{id}/mascotas")
    public PaginaResponse<MascotaResumenResponse> listarMascotas(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listarMascotas(id, search, activo, pageable);
    }
}
