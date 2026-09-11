package com.veterinaria.backend.vacuna.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.atencionmedica.repository.AtencionMedicaRepository;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import com.veterinaria.backend.mascota.exception.MascotaNoEncontradaException;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.vacuna.dto.AplicarVacunaRequest;
import com.veterinaria.backend.vacuna.dto.VacunaAplicadaResponse;
import com.veterinaria.backend.vacuna.dto.VacunaAplicadaResumenResponse;
import com.veterinaria.backend.vacuna.entity.Vacuna;
import com.veterinaria.backend.vacuna.entity.VacunaAplicada;
import com.veterinaria.backend.vacuna.exception.VacunaInactivaException;
import com.veterinaria.backend.vacuna.exception.VacunaNoEncontradaException;
import com.veterinaria.backend.vacuna.exception.VacunacionNoPermitidaException;
import com.veterinaria.backend.vacuna.mapper.VacunaMapper;
import com.veterinaria.backend.vacuna.repository.VacunaAplicadaRepository;
import com.veterinaria.backend.vacuna.repository.VacunaRepository;

@Service
public class VacunaAplicadaService {
    private static final int DEFAULT_SIZE = 20, MAX_SIZE = 100;
    private final AtencionMedicaRepository atenciones;
    private final VacunaRepository vacunas;
    private final VacunaAplicadaRepository aplicaciones;
    private final UsuarioRepository usuarios;
    private final UsuarioRolRepository roles;
    private final MascotaRepository mascotas;
    private final VacunaMapper mapper;

    @Autowired
    public VacunaAplicadaService(AtencionMedicaRepository atenciones, VacunaRepository vacunas,
            VacunaAplicadaRepository aplicaciones, UsuarioRepository usuarios, UsuarioRolRepository roles,
            MascotaRepository mascotas) {
        this.atenciones = atenciones; this.vacunas = vacunas; this.aplicaciones = aplicaciones;
        this.usuarios = usuarios; this.roles = roles; this.mascotas = mascotas; this.mapper = new VacunaMapper();
    }

    public VacunaAplicadaService(AtencionMedicaRepository atenciones, VacunaRepository vacunas,
            VacunaAplicadaRepository aplicaciones, UsuarioRepository usuarios, UsuarioRolRepository roles) {
        this(atenciones, vacunas, aplicaciones, usuarios, roles, null);
    }

    @Transactional
    public VacunaAplicadaResponse aplicar(Long atencionId, Long actorId, AplicarVacunaRequest request) {
        AtencionMedica atencion = atenciones.findByIdForVacunacion(atencionId)
                .orElseThrow(() -> new com.veterinaria.backend.atencionmedica.exception.AtencionMedicaNoEncontradaException("Atencion medica no encontrada."));
        validarAutoridad(atencion, actorId);
        if (atencion.getCita() == null || atencion.getCita().getTipoCita() != TipoCita.MEDICA
                || atencion.getCita().getEstado() != EstadoCita.ATENDIDA) {
            throw new VacunacionNoPermitidaException("La atencion no es apta para vacunacion.");
        }
        if (request == null || request.vacunaId() == null) throw new IllegalArgumentException("La vacuna es obligatoria.");
        Vacuna vacuna = vacunas.findById(request.vacunaId())
                .orElseThrow(() -> new VacunaNoEncontradaException("Vacuna no encontrada."));
        if (!Boolean.TRUE.equals(vacuna.getActivo())) throw new VacunaInactivaException("La vacuna esta inactiva.");
        if (atencion.getFechaAtencion() == null) throw new VacunacionNoPermitidaException("La atencion no tiene fecha.");
        LocalDate fecha = atencion.getFechaAtencion().toLocalDate();
        if (request.proximaFecha() != null && request.proximaFecha().isBefore(fecha)) throw new IllegalArgumentException("La proxima fecha no puede ser anterior a la fecha de aplicacion.");
        VacunaAplicada aplicada = new VacunaAplicada(); aplicada.setAtencionMedica(atencion); aplicada.setVacuna(vacuna); aplicada.setFechaAplicacion(fecha); aplicada.setProximaFecha(request.proximaFecha()); aplicada.setLote(trim(request.lote())); aplicada.setObservaciones(trim(request.observaciones()));
        return mapper.toAplicadaResponse(aplicaciones.save(aplicada));
    }

    @Transactional(readOnly = true)
    public List<VacunaAplicadaResponse> listarPorAtencion(Long atencionId) {
        if (!atenciones.existsById(atencionId)) throw new VacunacionNoPermitidaException("La atencion medica no existe.");
        return aplicaciones.findByAtencionMedicaIdOrderByFechaAplicacionDescIdDesc(atencionId).stream().map(mapper::toAplicadaResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VacunaAplicadaResumenResponse> historial(Long mascotaId, LocalDate desde, LocalDate hasta, Long vacunaId, Integer page, Integer size) {
        if (mascotas != null && !mascotas.existsById(mascotaId)) throw new MascotaNoEncontradaException("Mascota no encontrada.");
        int p = page == null ? 0 : page, s = size == null ? DEFAULT_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_SIZE) throw new IllegalArgumentException("La paginacion es invalida.");
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("El rango de fechas es invalido.");
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Order.desc("fechaAplicacion"), Sort.Order.desc("id")));
        Page<VacunaAplicada> result = aplicaciones.findHistorial(mascotaId, desde, hasta, vacunaId, pageable);
        return new PaginaResponse<>(result.getContent().stream().map(mapper::toAplicadaResumen).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private void validarAutoridad(AtencionMedica atencion, Long actorId) {
        if (actorId == null || atencion.getVeterinario() == null || !actorId.equals(atencion.getVeterinario().getId())) throw new AccessDeniedException("Acceso denegado.");
        var usuario = usuarios.findById(actorId).orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || !roles.existsByUsuario_IdAndRol_Nombre(actorId, NombreRol.VETERINARIO)) throw new AccessDeniedException("Acceso denegado.");
    }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
