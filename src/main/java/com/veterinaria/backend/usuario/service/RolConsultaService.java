package com.veterinaria.backend.usuario.service;

import java.util.List;

import com.veterinaria.backend.usuario.dto.RolResponse;
import com.veterinaria.backend.usuario.mapper.UsuarioMapper;
import com.veterinaria.backend.usuario.repository.RolRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolConsultaService {

    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;

    public RolConsultaService(RolRepository rolRepository, UsuarioMapper usuarioMapper) {
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarActivos() {
        return rolRepository.findAllByActivoTrueOrderByNombreAsc()
                .stream()
                .map(usuarioMapper::toRolResponse)
                .toList();
    }
}
