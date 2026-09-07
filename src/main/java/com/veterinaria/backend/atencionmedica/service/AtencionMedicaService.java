package com.veterinaria.backend.atencionmedica.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResponse;
import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResumenResponse;
import com.veterinaria.backend.atencionmedica.dto.CrearAtencionMedicaRequest;
import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.atencionmedica.exception.AtencionMedicaConflictException;
import com.veterinaria.backend.atencionmedica.exception.AtencionMedicaNoEncontradaException;
import com.veterinaria.backend.atencionmedica.mapper.AtencionMedicaMapper;
import com.veterinaria.backend.atencionmedica.repository.AtencionMedicaRepository;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.mascota.exception.MascotaNoEncontradaException;

@Service
public class AtencionMedicaService {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private final AtencionMedicaRepository atencionRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final MascotaRepository mascotaRepository;
    private final AtencionMedicaMapper mapper;

    @Autowired
    public AtencionMedicaService(AtencionMedicaRepository atencionRepository, CitaRepository citaRepository,
            UsuarioRolRepository usuarioRolRepository, AtencionMedicaMapper mapper, MascotaRepository mascotaRepository) {
        this.atencionRepository = atencionRepository;
        this.citaRepository = citaRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.mapper = mapper;
        this.mascotaRepository = mascotaRepository;
    }

    public AtencionMedicaService(AtencionMedicaRepository atencionRepository, CitaRepository citaRepository,
            UsuarioRolRepository usuarioRolRepository) {
        this(atencionRepository, citaRepository, usuarioRolRepository, new AtencionMedicaMapper(), null);
    }

    @Transactional
    public AtencionMedicaResponse crear(Long citaId, Long actorId, CrearAtencionMedicaRequest request) {
        Cita cita = citaRepository.findByIdForUpdate(citaId).orElseThrow(
                () -> new com.veterinaria.backend.cita.exception.CitaNoEncontradaException("Cita no encontrada."));
        if (cita.getTipoCita() != TipoCita.MEDICA) {
            throw new AtencionMedicaConflictException("Solo las citas medicas pueden tener atencion medica.");
        }
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new AtencionMedicaConflictException("La cita debe estar CONFIRMADA para registrar la atencion medica.");
        }
        if (actorId == null || cita.getTrabajadorAsignado() == null
                || !actorId.equals(cita.getTrabajadorAsignado().getId())
                || !Boolean.TRUE.equals(cita.getTrabajadorAsignado().getActivo())
                || !usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(actorId, NombreRol.VETERINARIO)) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        if (atencionRepository.findByCitaId(citaId).isPresent()) {
            throw new AtencionMedicaConflictException("La cita ya tiene una atencion medica.");
        }
        CrearAtencionMedicaRequest normalized = normalizeAndValidate(request);
        AtencionMedica atencion = new AtencionMedica();
        atencion.setCita(cita);
        atencion.setVeterinario(cita.getTrabajadorAsignado());
        atencion.setPesoKg(normalized.pesoKg());
        atencion.setTemperaturaC(normalized.temperaturaC());
        atencion.setSintomas(normalized.sintomas());
        atencion.setDiagnostico(normalized.diagnostico());
        atencion.setMotivoSinDiagnostico(normalized.motivoSinDiagnostico());
        atencion.setTratamiento(normalized.tratamiento());
        atencion.setMotivoSinTratamiento(normalized.motivoSinTratamiento());
        atencion.setReceta(normalized.receta());
        atencion.setMotivoSinReceta(normalized.motivoSinReceta());
        atencion.setObservaciones(normalized.observaciones());
        try {
            AtencionMedica saved = atencionRepository.saveAndFlush(atencion);
            cita.setEstado(EstadoCita.ATENDIDA);
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new AtencionMedicaConflictException("La cita ya tiene una atencion medica.");
        }
    }

    @Transactional(readOnly = true)
    public AtencionMedicaResponse obtenerPorCita(Long citaId, Long actorId, Set<NombreRol> roles) {
        validarLectura(roles);
        AtencionMedica atencion = atencionRepository.findByCitaId(citaId)
                .orElseThrow(() -> new AtencionMedicaNoEncontradaException("Atencion medica no encontrada."));
        return mapper.toResponse(atencion);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AtencionMedicaResumenResponse> historial(Long mascotaId, Long actorId, Set<NombreRol> roles,
            LocalDateTime desde, LocalDateTime hasta, Integer page, Integer size) {
        validarLectura(roles);
        if (mascotaRepository != null && !mascotaRepository.existsById(mascotaId)) {
            throw new MascotaNoEncontradaException("Mascota no encontrada.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("El rango de fechas es invalido.");
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAX_SIZE) throw new IllegalArgumentException("La paginacion es invalida.");
        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("fechaAtencion"), Sort.Order.desc("id")));
        Page<AtencionMedica> result;
        if (desde == null && hasta == null) result = atencionRepository.findHistorial(mascotaId, pageable);
        else if (desde != null && hasta == null) result = atencionRepository.findHistorialDesde(mascotaId, desde, pageable);
        else if (desde == null) result = atencionRepository.findHistorialHasta(mascotaId, hasta, pageable);
        else result = atencionRepository.findHistorialEntre(mascotaId, desde, hasta, pageable);
        return new PaginaResponse<>(result.getContent().stream().map(mapper::toResumen).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private void validarLectura(Set<NombreRol> roles) {
        if (roles == null || (!roles.contains(NombreRol.VETERINARIO) && !roles.contains(NombreRol.ADMINISTRADOR))) {
            throw new AccessDeniedException("Acceso denegado.");
        }
    }

    private CrearAtencionMedicaRequest normalizeAndValidate(CrearAtencionMedicaRequest request) {
        if (request == null) throw new IllegalArgumentException("Los datos clinicos son obligatorios.");
        String diagnostico = trim(request.diagnostico()), motivoDiagnostico = trim(request.motivoSinDiagnostico());
        String tratamiento = trim(request.tratamiento()), motivoTratamiento = trim(request.motivoSinTratamiento());
        String receta = trim(request.receta()), motivoReceta = trim(request.motivoSinReceta());
        if (diagnostico == null && motivoDiagnostico == null) throw new IllegalArgumentException("Debe informar diagnostico o motivo sin diagnostico.");
        if (tratamiento == null && motivoTratamiento == null) throw new IllegalArgumentException("Debe informar tratamiento o motivo sin tratamiento.");
        if (receta == null && motivoReceta == null) throw new IllegalArgumentException("Debe informar receta o motivo sin receta.");
        return new CrearAtencionMedicaRequest(request.pesoKg(), request.temperaturaC(), trim(request.sintomas()), diagnostico,
                motivoDiagnostico, tratamiento, motivoTratamiento, receta, motivoReceta, trim(request.observaciones()));
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
