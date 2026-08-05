package com.veterinaria.backend.horario.service;

import java.time.LocalTime;
import java.util.List;

import com.veterinaria.backend.horario.dto.ActualizarHorarioRequest;
import com.veterinaria.backend.horario.dto.CambiarDisponibilidadRequest;
import com.veterinaria.backend.horario.dto.CrearHorarioRequest;
import com.veterinaria.backend.horario.dto.HorarioTrabajadorResponse;
import com.veterinaria.backend.horario.entity.HorarioTrabajador;
import com.veterinaria.backend.horario.exception.HorarioNoEncontradoException;
import com.veterinaria.backend.horario.exception.HorarioSolapadoException;
import com.veterinaria.backend.horario.exception.RangoHorarioInvalidoException;
import com.veterinaria.backend.horario.exception.TrabajadorNoEncontradoException;
import com.veterinaria.backend.horario.exception.TrabajadorNoProgramableException;
import com.veterinaria.backend.horario.mapper.HorarioTrabajadorMapper;
import com.veterinaria.backend.horario.repository.HorarioTrabajadorRepository;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HorarioTrabajadorService {
    private static final List<NombreRol> ROLES_PROGRAMABLES = List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO);
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final HorarioTrabajadorRepository horarioRepository;
    private final HorarioTrabajadorMapper mapper;

    public HorarioTrabajadorService(UsuarioRepository usuarioRepository, UsuarioRolRepository usuarioRolRepository,
            HorarioTrabajadorRepository horarioRepository, HorarioTrabajadorMapper mapper) {
        this.usuarioRepository = usuarioRepository; this.usuarioRolRepository = usuarioRolRepository;
        this.horarioRepository = horarioRepository; this.mapper = mapper;
    }

    @Transactional
    public HorarioTrabajadorResponse crear(Long trabajadorId, CrearHorarioRequest request) {
        Usuario usuario = requireProgramable(trabajadorId);
        validateRange(request.diaSemana(), request.horaInicio(), request.horaFin(), request.descansoInicio(), request.descansoFin());
        if (horarioRepository.existsSolapamiento(trabajadorId, request.diaSemana(), request.horaInicio(), request.horaFin(), null)) {
            throw new HorarioSolapadoException("El horario se solapa con otro horario.");
        }
        HorarioTrabajador horario = new HorarioTrabajador(); horario.setUsuario(usuario); copy(horario, request.diaSemana(), request.horaInicio(), request.horaFin(), request.descansoInicio(), request.descansoFin()); horario.setDisponible(true);
        return mapper.toResponse(horarioRepository.save(horario));
    }

    @Transactional(readOnly = true)
    public List<HorarioTrabajadorResponse> listar(Long trabajadorId) {
        requireProgramable(trabajadorId);
        return horarioRepository.findByUsuarioIdOrderByDiaSemanaAscHoraInicioAsc(trabajadorId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HorarioTrabajadorResponse obtener(Long trabajadorId, Long horarioId) {
        requireProgramable(trabajadorId);
        return mapper.toResponse(horarioRepository.findByIdAndUsuarioId(horarioId, trabajadorId)
                .orElseThrow(() -> new HorarioNoEncontradoException("Horario no encontrado.")));
    }

    @Transactional
    public HorarioTrabajadorResponse actualizar(Long trabajadorId, Long horarioId, ActualizarHorarioRequest request) {
        requireProgramable(trabajadorId);
        HorarioTrabajador horario = horarioRepository.findByIdAndUsuarioId(horarioId, trabajadorId)
                .orElseThrow(() -> new HorarioNoEncontradoException("Horario no encontrado."));
        validateRange(request.diaSemana(), request.horaInicio(), request.horaFin(), request.descansoInicio(), request.descansoFin());
        if (horarioRepository.existsSolapamiento(trabajadorId, request.diaSemana(), request.horaInicio(), request.horaFin(), horarioId)) throw new HorarioSolapadoException("El horario se solapa con otro horario.");
        copy(horario, request.diaSemana(), request.horaInicio(), request.horaFin(), request.descansoInicio(), request.descansoFin());
        return mapper.toResponse(horario);
    }

    @Transactional
    public HorarioTrabajadorResponse cambiarDisponibilidad(Long trabajadorId, Long horarioId, CambiarDisponibilidadRequest request) {
        requireProgramable(trabajadorId);
        HorarioTrabajador horario = horarioRepository.findByIdAndUsuarioId(horarioId, trabajadorId)
                .orElseThrow(() -> new HorarioNoEncontradoException("Horario no encontrado."));
        horario.setDisponible(request.disponible());
        return mapper.toResponse(horario);
    }

    private Usuario requireProgramable(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new TrabajadorNoEncontradoException("Trabajador no encontrado."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || !usuarioRolRepository.existsByUsuario_IdAndRol_NombreIn(id, ROLES_PROGRAMABLES)) throw new TrabajadorNoProgramableException("El usuario no es un trabajador programable activo.");
        return usuario;
    }
    private void copy(HorarioTrabajador h, Integer day, LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) { h.setDiaSemana(day); h.setHoraInicio(start); h.setHoraFin(end); h.setDescansoInicio(breakStart); h.setDescansoFin(breakEnd); }
    private void validateRange(Integer day, LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        if (day == null || day < 1 || day > 7 || start == null || end == null || !start.isBefore(end)) throw new RangoHorarioInvalidoException("El rango horario no es valido.");
        if ((breakStart == null) != (breakEnd == null) || (breakStart != null && (!breakStart.isBefore(breakEnd) || breakStart.isBefore(start) || breakEnd.isAfter(end)))) throw new RangoHorarioInvalidoException("El descanso debe estar dentro de la jornada.");
    }
}
