package com.veterinaria.backend.comprobante.service;

import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veterinaria.backend.comprobante.dto.*;
import com.veterinaria.backend.comprobante.entity.Comprobante;
import com.veterinaria.backend.comprobante.enums.*;
import com.veterinaria.backend.comprobante.exception.*;
import com.veterinaria.backend.comprobante.repository.ComprobanteRepository;
import com.veterinaria.backend.pago.entity.Pago;
import com.veterinaria.backend.pago.enums.EstadoPago;
import com.veterinaria.backend.pago.exception.PagoNoEncontradoException;
import com.veterinaria.backend.pago.repository.PagoRepository;
import com.veterinaria.backend.usuario.enums.NombreRol;

@Service
public class ComprobanteService {
    private final PagoRepository pagoRepository; private final ComprobanteRepository repository;
    public ComprobanteService(PagoRepository p, ComprobanteRepository r){this.pagoRepository=p;this.repository=r;}
    @Transactional
    public ComprobanteResponse emitir(Long pagoId, Set<NombreRol> roles, EmitirComprobanteRequest req){
        validar(roles); Pago pago=pagoRepository.findWithCitaAndRegistradoPorById(pagoId).orElseThrow(()->new PagoNoEncontradoException(pagoId));
        if(pago.getEstado()!=EstadoPago.PAGADO) throw new ComprobanteConflictException("Solo se puede emitir comprobante para pagos PAGADO.");
        if(repository.existsByPagoId(pagoId)) throw new ComprobanteConflictException("El pago ya tiene comprobante.");
        validarFactura(req); Comprobante c=new Comprobante(); c.setPago(pago); c.setTipoComprobante(req.tipoComprobante()); c.setSerie(trim(req.serie())); c.setNumero(trim(req.numero())); c.setRuc(trim(req.ruc())); c.setRazonSocial(trim(req.razonSocial())); c.setDireccionFiscal(trim(req.direccionFiscal())); c.setEstado(EstadoComprobante.EMITIDO);
        try{return toResponse(repository.saveAndFlush(c));}catch(DataIntegrityViolationException ex){throw new ComprobanteConflictException("El comprobante duplica el pago o la numeracion.");}
    }
    @Transactional(readOnly=true) public ComprobanteResponse porPago(Long pagoId, Set<NombreRol> roles){validar(roles); return repository.findWithPagoByPagoId(pagoId).map(this::toResponse).orElseThrow(()->new ComprobanteNoEncontradoException("No se encontro comprobante para el pago "+pagoId+"."));}
    @Transactional(readOnly=true) public ComprobanteResponse obtener(Long id, Set<NombreRol> roles){validar(roles); return repository.findWithPagoById(id).map(this::toResponse).orElseThrow(()->new ComprobanteNoEncontradoException("No se encontro el comprobante "+id+"."));}
    private void validarFactura(EmitirComprobanteRequest r){if(r.tipoComprobante()==TipoComprobante.FACTURA){if(r.ruc()==null||!r.ruc().trim().matches("\\d{11}"))throw new IllegalArgumentException("El RUC debe contener 11 digitos."); if(blank(r.razonSocial())||blank(r.direccionFiscal()))throw new IllegalArgumentException("Factura requiere razon social y direccion fiscal.");}}
    private ComprobanteResponse toResponse(Comprobante c){return new ComprobanteResponse(c.getId(),c.getPago().getId(),c.getPago().getCita().getId(),c.getTipoComprobante(),c.getSerie(),c.getNumero(),c.getRuc(),c.getRazonSocial(),c.getDireccionFiscal(),c.getFechaEmision(),c.getEstado(),c.getPago().getMontoTotal());}
    private void validar(Set<NombreRol> r){if(r==null||(!r.contains(NombreRol.ADMINISTRADOR)&&!r.contains(NombreRol.RECEPCIONISTA)))throw new AccessDeniedException("Acceso denegado.");}
    private String trim(String s){return s==null?null:(s.trim().isEmpty()?null:s.trim());} private boolean blank(String s){return trim(s)==null;}
}
