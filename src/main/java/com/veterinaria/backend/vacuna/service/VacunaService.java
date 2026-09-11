package com.veterinaria.backend.vacuna.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.veterinaria.backend.vacuna.dto.*;
import com.veterinaria.backend.vacuna.entity.Vacuna;
import com.veterinaria.backend.vacuna.exception.*;
import com.veterinaria.backend.vacuna.mapper.VacunaMapper;
import com.veterinaria.backend.vacuna.repository.VacunaRepository;
import com.veterinaria.backend.usuario.dto.PaginaResponse;

@Service
public class VacunaService {
    private static final int DEFAULT_SIZE = 20, MAX_SIZE = 100;
    private final VacunaRepository repository;
    private final VacunaMapper mapper;
    @Autowired
    public VacunaService(VacunaRepository repository, VacunaMapper mapper) { this.repository = repository; this.mapper = mapper; }
    public VacunaService(VacunaRepository repository) { this(repository, new VacunaMapper()); }

    @Transactional
    public VacunaResponse crear(CrearVacunaRequest request) {
        String nombre = required(request == null ? null : request.nombre());
        if (repository.existsByNombreIgnoreCase(nombre)) throw new VacunaDuplicadaException("La vacuna ya existe.");
        Vacuna v = new Vacuna(); v.setNombre(nombre); v.setDescripcion(trim(request.descripcion())); v.setActivo(true); return save(v);
    }
    @Transactional(readOnly = true)
    public PaginaResponse<VacunaResumenResponse> listar(Boolean activo, Integer page, Integer size) {
        int p = page == null ? 0 : page, s = size == null ? DEFAULT_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_SIZE) throw new IllegalArgumentException("La paginacion es invalida.");
        Page<Vacuna> result = activo == null ? repository.findAll(PageRequest.of(p, s, Sort.by("nombre").ascending().and(Sort.by("id").ascending()))) : repository.findByActivo(activo, PageRequest.of(p, s, Sort.by("nombre").ascending().and(Sort.by("id").ascending())));
        return new PaginaResponse<>(result.getContent().stream().map(mapper::toResumen).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    @Transactional(readOnly = true) public VacunaResponse obtener(Long id) { return mapper.toResponse(find(id)); }
    @Transactional
    public VacunaResponse actualizar(Long id, ActualizarVacunaRequest request) {
        Vacuna v = find(id); String nombre = required(request == null ? null : request.nombre());
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) throw new VacunaDuplicadaException("La vacuna ya existe.");
        v.setNombre(nombre); v.setDescripcion(trim(request.descripcion())); return save(v);
    }
    @Transactional
    public VacunaResponse cambiarEstado(Long id, CambiarEstadoVacunaRequest request) {
        Vacuna v = find(id); if (request == null || request.activo() == null) throw new IllegalArgumentException("El estado es obligatorio."); v.setActivo(request.activo()); return save(v);
    }
    private VacunaResponse save(Vacuna v) { try { return mapper.toResponse(repository.saveAndFlush(v)); } catch (DataIntegrityViolationException e) { throw new VacunaDuplicadaException("La vacuna ya existe."); } }
    private Vacuna find(Long id) { return repository.findById(id).orElseThrow(() -> new VacunaNoEncontradaException("Vacuna no encontrada.")); }
    private String required(String value) { String r = trim(value); if (r == null) throw new IllegalArgumentException("El nombre es obligatorio."); return r; }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
