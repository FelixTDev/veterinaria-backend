package com.veterinaria.backend.mascota.controller;

import java.net.URI;

import com.veterinaria.backend.mascota.dto.ActualizarMascotaRequest;
import com.veterinaria.backend.mascota.dto.CambiarEstadoMascotaRequest;
import com.veterinaria.backend.mascota.dto.CrearMascotaRequest;
import com.veterinaria.backend.mascota.dto.MascotaDetalleResponse;
import com.veterinaria.backend.mascota.dto.MascotaResumenResponse;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;
import com.veterinaria.backend.mascota.service.MascotaGestionService;
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
@RequestMapping("/api/v1/mascotas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class MascotaController {

    private final MascotaGestionService service;

    public MascotaController(MascotaGestionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MascotaDetalleResponse> crear(@Valid @RequestBody CrearMascotaRequest request) {
        MascotaDetalleResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/mascotas/" + response.id())).body(response);
    }

    @GetMapping
    public PaginaResponse<MascotaResumenResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EspecieMascota especie,
            @RequestParam(required = false) SexoMascota sexo,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(search, activo, clienteId, especie, sexo, pageable);
    }

    @GetMapping("/{id}")
    public MascotaDetalleResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PutMapping("/{id}")
    public MascotaDetalleResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarMascotaRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public MascotaDetalleResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoMascotaRequest request) {
        return service.cambiarEstado(id, request);
    }
}
