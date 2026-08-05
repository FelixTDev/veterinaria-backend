package com.veterinaria.backend.horario.service;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.horario.dto.ActualizarIndisponibilidadRequest;
import com.veterinaria.backend.horario.dto.CrearIndisponibilidadRequest;
import com.veterinaria.backend.horario.dto.IndisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.entity.IndisponibilidadTrabajador;
import com.veterinaria.backend.horario.exception.IndisponibilidadNoEncontradaException;
import com.veterinaria.backend.horario.exception.IndisponibilidadSolapadaException;
import com.veterinaria.backend.horario.exception.RangoIndisponibilidadInvalidoException;
import com.veterinaria.backend.horario.mapper.IndisponibilidadTrabajadorMapper;
import com.veterinaria.backend.horario.repository.IndisponibilidadTrabajadorRepository;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndisponibilidadTrabajadorService {
    private static final List<NombreRol> ROLES_PROGRAMABLES = List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO);
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final IndisponibilidadTrabajadorRepository repository;
    private final IndisponibilidadTrabajadorMapper mapper;

    public IndisponibilidadTrabajadorService(UsuarioRepository usuarioRepository, UsuarioRolRepository usuarioRolRepository,
            IndisponibilidadTrabajadorRepository repository, IndisponibilidadTrabajadorMapper mapper) {
        this.usuarioRepository = usuarioRepository; this.usuarioRolRepository = usuarioRolRepository; this.repository = repository; this.mapper = mapper;
    }

    @Transactional
    public IndisponibilidadTrabajadorResponse crear(Long trabajadorId, CrearIndisponibilidadRequest request) {
        Usuario usuario = requireProgramable(trabajadorId);
        validateFutureRange(request.fechaInicio(), request.fechaFin());
        if (repository.existsSolapamiento(trabajadorId, request.fechaInicio(), request.fechaFin(), null)) throw new IndisponibilidadSolapadaException("La indisponibilidad se solapa con otra.");
        IndisponibilidadTrabajador value = new IndisponibilidadTrabajador(); value.setUsuario(usuario); copy(value, request.fechaInicio(), request.fechaFin(), request.motivo());
        return mapper.toResponse(repository.save(value));
    }

    @Transactional(readOnly = true)
    public List<IndisponibilidadTrabajadorResponse> listar(Long trabajadorId) {
        requireProgramable(trabajadorId);
        return repository.findByUsuarioIdOrderByFechaInicioAsc(trabajadorId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IndisponibilidadTrabajadorResponse obtener(Long trabajadorId, Long id) {
        requireProgramable(trabajadorId);
        return mapper.toResponse(repository.findByIdAndUsuarioId(id, trabajadorId).orElseThrow(() -> new IndisponibilidadNoEncontradaException("Indisponibilidad no encontrada.")));
    }

    @Transactional
    public IndisponibilidadTrabajadorResponse actualizar(Long trabajadorId, Long id, ActualizarIndisponibilidadRequest request) {
        requireProgramable(trabajadorId);
        IndisponibilidadTrabajador value = repository.findByIdAndUsuarioId(id, trabajadorId).orElseThrow(() -> new IndisponibilidadNoEncontradaException("Indisponibilidad no encontrada."));
        validateFutureRange(request.fechaInicio(), request.fechaFin());
        if (repository.existsSolapamiento(trabajadorId, request.fechaInicio(), request.fechaFin(), id)) throw new IndisponibilidadSolapadaException("La indisponibilidad se solapa con otra.");
        copy(value, request.fechaInicio(), request.fechaFin(), request.motivo());
        return mapper.toResponse(value);
    }

    private Usuario requireProgramable(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new com.veterinaria.backend.horario.exception.TrabajadorNoEncontradoException("Trabajador no encontrado."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || !usuarioRolRepository.existsByUsuario_IdAndRol_NombreIn(id, ROLES_PROGRAMABLES)) throw new com.veterinaria.backend.horario.exception.TrabajadorNoProgramableException("El usuario no es un trabajador programable activo.");
        return usuario;
    }
    private void validateFutureRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end) || start.isBefore(LocalDateTime.now())) throw new RangoIndisponibilidadInvalidoException("El rango de indisponibilidad no es valido o ya paso.");
    }
    private void copy(IndisponibilidadTrabajador value, LocalDateTime start, LocalDateTime end, String motive) { value.setFechaInicio(start); value.setFechaFin(end); value.setMotivo(motive.trim()); }
}
