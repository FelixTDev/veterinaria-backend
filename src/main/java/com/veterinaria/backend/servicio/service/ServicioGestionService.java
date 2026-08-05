package com.veterinaria.backend.servicio.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.veterinaria.backend.servicio.dto.ActualizarServicioRequest;
import com.veterinaria.backend.servicio.dto.CambiarEstadoServicioRequest;
import com.veterinaria.backend.servicio.dto.ConfigurarPreciosRequest;
import com.veterinaria.backend.servicio.dto.CrearServicioRequest;
import com.veterinaria.backend.servicio.dto.PrecioServicioRequest;
import com.veterinaria.backend.servicio.dto.PrecioServicioResponse;
import com.veterinaria.backend.servicio.dto.ServicioDetalleResponse;
import com.veterinaria.backend.servicio.dto.ServicioResumenResponse;
import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TamanoMascota;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.exception.PrecioServicioInvalidoException;
import com.veterinaria.backend.servicio.exception.ServicioDuplicadoException;
import com.veterinaria.backend.servicio.exception.ServicioNoEncontradoException;
import com.veterinaria.backend.servicio.mapper.ServicioMapper;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioGestionService {

    private final ServicioRepository servicioRepository;
    private final PrecioServicioTamanoRepository precioRepository;
    private final ServicioMapper mapper;

    public ServicioGestionService(ServicioRepository servicioRepository, PrecioServicioTamanoRepository precioRepository,
            ServicioMapper mapper) {
        this.servicioRepository = servicioRepository;
        this.precioRepository = precioRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ServicioDetalleResponse crear(CrearServicioRequest request) {
        String nombre = normalize(request.nombre());
        if (servicioRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ServicioDuplicadoException("El servicio ya se encuentra registrado.");
        }
        Servicio servicio = new Servicio();
        copy(servicio, nombre, request.descripcion(), request.tipoServicio(), request.precioBase(), request.duracionMinutos());
        servicio.setActivo(Boolean.TRUE);
        return mapper.toDetalle(servicioRepository.save(servicio), List.of());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ServicioResumenResponse> listar(String search, Boolean activo, TipoServicio tipoServicio,
            Pageable pageable) {
        validatePage(pageable);
        Page<Servicio> page = servicioRepository.findAllForGestion(normalizeNullable(search), activo, tipoServicio, pageable);
        Map<Long, Long> counts = new HashMap<>();
        List<Long> ids = page.getContent().stream().map(Servicio::getId).toList();
        if (!ids.isEmpty()) {
            precioRepository.countActivosByServicioIds(ids).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        }
        List<ServicioResumenResponse> content = page.getContent().stream()
                .map(item -> mapper.toResumen(item, counts.getOrDefault(item.getId(), 0L) > 0)).toList();
        return new PaginaResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ServicioDetalleResponse obtener(Long id) {
        Servicio servicio = findServicio(id);
        return mapper.toDetalle(servicio, precioRepository.findByServicioIdOrderByTamanoMascotaAsc(id));
    }

    @Transactional
    public ServicioDetalleResponse actualizar(Long id, ActualizarServicioRequest request) {
        Servicio servicio = findServicio(id);
        String nombre = normalize(request.nombre());
        if (servicioRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ServicioDuplicadoException("El servicio ya se encuentra registrado.");
        }
        copy(servicio, nombre, request.descripcion(), request.tipoServicio(), request.precioBase(), request.duracionMinutos());
        return mapper.toDetalle(servicio, precioRepository.findByServicioIdOrderByTamanoMascotaAsc(id));
    }

    @Transactional
    public ServicioDetalleResponse cambiarEstado(Long id, CambiarEstadoServicioRequest request) {
        Servicio servicio = findServicio(id);
        servicio.setActivo(request.activo());
        return mapper.toDetalle(servicio, precioRepository.findByServicioIdOrderByTamanoMascotaAsc(id));
    }

    @Transactional(readOnly = true)
    public List<PrecioServicioResponse> listarPrecios(Long servicioId) {
        findServicio(servicioId);
        return precioRepository.findByServicioIdOrderByTamanoMascotaAsc(servicioId).stream().map(mapper::toPrecio).toList();
    }

    @Transactional
    public List<PrecioServicioResponse> configurarPrecios(Long servicioId, ConfigurarPreciosRequest request) {
        Servicio servicio = findServicio(servicioId);
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new PrecioServicioInvalidoException("No se pueden configurar precios de un servicio inactivo.");
        }
        Set<TamanoMascota> sizes = new HashSet<>();
        for (PrecioServicioRequest item : request.precios()) {
            if (!sizes.add(item.tamanoMascota())) {
                throw new PrecioServicioInvalidoException("No se permiten tamaños duplicados.");
            }
        }
        List<PrecioServicioTamano> existentes = precioRepository.findByServicioId(servicioId);
        Map<TamanoMascota, PrecioServicioTamano> bySize = new HashMap<>();
        existentes.forEach(item -> bySize.put(item.getTamanoMascota(), item));
        List<PrecioServicioTamano> toSave = new ArrayList<>();
        for (PrecioServicioRequest item : request.precios()) {
            PrecioServicioTamano precio = bySize.getOrDefault(item.tamanoMascota(), new PrecioServicioTamano());
            precio.setServicio(servicio);
            precio.setTamanoMascota(item.tamanoMascota());
            precio.setPrecio(item.precio());
            precio.setDuracionMinutos(item.duracionMinutos());
            precio.setActivo(Boolean.TRUE);
            toSave.add(precio);
        }
        existentes.stream().filter(item -> !sizes.contains(item.getTamanoMascota())).forEach(item -> {
            item.setActivo(Boolean.FALSE);
            toSave.add(item);
        });
        return precioRepository.saveAll(toSave).stream().map(mapper::toPrecio).toList();
    }

    private Servicio findServicio(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ServicioNoEncontradoException("Servicio no encontrado."));
    }
    private void copy(Servicio s, String nombre, String descripcion, TipoServicio tipo, java.math.BigDecimal precio, Integer duracion) {
        s.setNombre(nombre);
        s.setDescripcion(normalizeNullable(descripcion));
        s.setTipoServicio(tipo);
        s.setPrecioBase(precio);
        s.setDuracionMinutos(duracion);
    }
    private String normalize(String value) { return value.trim(); }
    private String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void validatePage(Pageable pageable) { if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) throw new IllegalArgumentException("El tamaño de pagina debe estar entre 1 y 100."); }
}
