package com.veterinaria.backend.usuario.controller;

import java.util.List;

import com.veterinaria.backend.usuario.dto.RolResponse;
import com.veterinaria.backend.usuario.service.RolConsultaService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolController {

    private final RolConsultaService rolConsultaService;

    public RolController(RolConsultaService rolConsultaService) {
        this.rolConsultaService = rolConsultaService;
    }

    @GetMapping
    public List<RolResponse> listarActivos() {
        return rolConsultaService.listarActivos();
    }
}
