package com.veterinaria.backend.pago.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.exception.CitaNoEncontradaException;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.repository.CitaServicioRepository;
import com.veterinaria.backend.pago.dto.*;
import com.veterinaria.backend.pago.entity.*;
import com.veterinaria.backend.pago.enums.EstadoPago;
import com.veterinaria.backend.pago.exception.*;
import com.veterinaria.backend.pago.repository.*;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.exception.UsuarioNoEncontradoException;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;

@Service
public class PagoService {
    private final CitaRepository citaRepository;
    private final CitaServicioRepository citaServicioRepository;
    private final PagoRepository pagoRepository;
    private final DetallePagoRepository detalleRepository;
    private final UsuarioRepository usuarioRepository;

    public PagoService(CitaRepository citaRepository, CitaServicioRepository citaServicioRepository,
            PagoRepository pagoRepository, DetallePagoRepository detalleRepository, UsuarioRepository usuarioRepository) {
        this.citaRepository = citaRepository; this.citaServicioRepository = citaServicioRepository;
        this.pagoRepository = pagoRepository; this.detalleRepository = detalleRepository; this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PagoResponse registrar(Long citaId, Long actorId, Set<NombreRol> roles, RegistrarPagoRequest request) {
        validarRol(roles);
        Usuario actor = usuarioRepository.findById(actorId).orElseThrow(() -> new UsuarioNoEncontradoException("No se encontro el usuario " + actorId + "."));
        if (!Boolean.TRUE.equals(actor.getActivo())) throw new AccessDeniedException("El usuario no esta activo.");
        Cita cita = citaRepository.findByIdForUpdate(citaId).orElseThrow(() -> new CitaNoEncontradaException("No se encontro la cita " + citaId + "."));
        if (cita.getEstado() != EstadoCita.ATENDIDA) throw new PagoConflictException("Solo se pueden registrar pagos de citas atendidas.");
        BigDecimal total = citaServicioRepository.sumPrecioAplicadoByCitaId(citaId);
        BigDecimal pagado = Optional.ofNullable(pagoRepository.sumPagadoByCitaId(citaId)).orElse(BigDecimal.ZERO);
        BigDecimal monto = request.detalles().stream().map(DetallePagoRequest::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = total.subtract(pagado);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) throw new PagoConflictException("El monto del pago debe ser mayor que cero.");
        if (saldo.compareTo(BigDecimal.ZERO) <= 0 || monto.compareTo(saldo) > 0) throw new PagoConflictException("El pago excede el saldo pendiente.");
        Pago pago = new Pago(); pago.setCita(cita); pago.setRegistradoPor(actor); pago.setMontoTotal(monto);
        pago.setEstado(EstadoPago.PAGADO); pago.setFechaPago(LocalDateTime.now()); pago.setObservaciones(normalize(request.observaciones()));
        try {
            Pago saved = pagoRepository.saveAndFlush(pago);
            List<DetallePago> details = request.detalles().stream().map(d -> {
                DetallePago detail = new DetallePago(); detail.setPago(saved); detail.setMedioPago(d.medioPago()); detail.setMonto(d.monto()); detail.setReferencia(normalize(d.referencia())); return detail;
            }).toList();
            detalleRepository.saveAllAndFlush(details);
            return toResponse(saved, details);
        } catch (DataIntegrityViolationException ex) { throw new PagoConflictException("No se pudo registrar el pago por una restriccion de datos."); }
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PagoResponse> listar(Long citaId, Integer page, Integer size, Set<NombreRol> roles) {
        validarRolConsulta(roles); validarCita(citaId);
        if (page != null && page < 0 || size != null && (size < 1 || size > 100)) throw new IllegalArgumentException("La paginacion es invalida.");
        Page<Pago> result = pagoRepository.findAllByCitaId(citaId, PageRequest.of(page == null ? 0 : page, size == null ? 20 : Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, List<DetallePago>> details = cargarDetalles(result.getContent());
        return new PaginaResponse<>(result.getContent().stream().map(p -> toResponse(p, details.getOrDefault(p.getId(), List.of()))).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagoResponse obtener(Long id, Set<NombreRol> roles) {
        validarRolConsulta(roles); Pago pago = pagoRepository.findWithCitaAndRegistradoPorById(id).orElseThrow(() -> new PagoNoEncontradoException(id));
        return toResponse(pago, detalleRepository.findAllByPagoIdOrderByIdAsc(id));
    }

    private Map<Long, List<DetallePago>> cargarDetalles(List<Pago> pagos) { if (pagos.isEmpty()) return Map.of(); Map<Long,List<DetallePago>> map = new HashMap<>(); for (DetallePago d : detalleRepository.findAllByPagoIdIn(pagos.stream().map(Pago::getId).toList())) map.computeIfAbsent(d.getPago().getId(), k -> new ArrayList<>()).add(d); return map; }
    private PagoResponse toResponse(Pago p, List<DetallePago> details) { return new PagoResponse(p.getId(), p.getCita().getId(), p.getRegistradoPor().getId(), p.getMontoTotal(), p.getEstado(), p.getFechaPago(), p.getObservaciones(), details.stream().map(d -> new DetallePagoResponse(d.getId(), d.getMedioPago(), d.getMonto(), d.getReferencia())).toList()); }
    private void validarCita(Long id) { if (!citaRepository.existsById(id)) throw new CitaNoEncontradaException("No se encontro la cita " + id + "."); }
    private void validarRol(Set<NombreRol> roles) { if (roles == null || (!roles.contains(NombreRol.ADMINISTRADOR) && !roles.contains(NombreRol.RECEPCIONISTA))) throw new AccessDeniedException("Acceso denegado."); }
    private void validarRolConsulta(Set<NombreRol> roles) { validarRol(roles); }
    private String normalize(String value) { if (value == null) return null; String t = value.trim(); return t.isEmpty() ? null : t; }
}
