package com.veterinaria.backend.cliente.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.veterinaria.backend.cliente.dto.ActualizarClienteRequest;
import com.veterinaria.backend.cliente.dto.CambiarEstadoClienteRequest;
import com.veterinaria.backend.cliente.dto.ClienteDetalleResponse;
import com.veterinaria.backend.cliente.dto.ClienteResumenResponse;
import com.veterinaria.backend.cliente.dto.CrearClienteRequest;
import com.veterinaria.backend.cliente.exception.ClienteNoEncontradoException;
import com.veterinaria.backend.cliente.exception.DocumentoClienteDuplicadoException;
import com.veterinaria.backend.cliente.exception.PageSizeClienteInvalidoException;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.mapper.ClienteMapper;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.dto.MascotaResumenResponse;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteGestionService {

    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final ClienteMapper clienteMapper;

    public ClienteGestionService(ClienteRepository clienteRepository, MascotaRepository mascotaRepository,
            ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
        this.clienteMapper = clienteMapper;
    }

    @Transactional
    public ClienteDetalleResponse crear(CrearClienteRequest request) {
        String documento = normalizar(request.numeroDocumento());
        if (clienteRepository.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), documento)) {
            throw new DocumentoClienteDuplicadoException("El documento ya se encuentra registrado.");
        }
        Cliente cliente = new Cliente();
        copiar(cliente, request.primerNombre(), request.segundoNombre(), request.primerApellido(),
                request.segundoApellido(), request.tipoDocumento(), documento, request.fechaNacimiento(),
                request.telefono(), request.correo());
        cliente.setActivo(Boolean.TRUE);
        Cliente saved = clienteRepository.save(cliente);
        return clienteMapper.toDetalle(saved, List.of());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResumenResponse> listar(String search, Boolean activo, TipoDocumento tipoDocumento,
            Pageable pageable) {
        validarPagina(pageable);
        validarOrden(pageable, "id", "primerNombre", "primerApellido", "numeroDocumento", "correo", "activo");
        Page<Cliente> page = clienteRepository.findAllForGestion(normalizarNullable(search), activo, tipoDocumento, pageable);
        List<Long> ids = page.getContent().stream().map(Cliente::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            clienteRepository.countMascotasByClienteIds(ids).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        }
        List<ClienteResumenResponse> content = page.getContent().stream()
                .map(cliente -> clienteMapper.toResumen(cliente, counts.getOrDefault(cliente.getId(), 0L))).toList();
        return new PaginaResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ClienteDetalleResponse obtener(Long id) {
        Cliente cliente = findCliente(id);
        return clienteMapper.toDetalle(cliente, mascotaRepository.findByClienteId(id));
    }

    @Transactional
    public ClienteDetalleResponse actualizar(Long id, ActualizarClienteRequest request) {
        Cliente cliente = findCliente(id);
        String documento = normalizar(request.numeroDocumento());
        if (clienteRepository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                request.tipoDocumento(), documento, id)) {
            throw new DocumentoClienteDuplicadoException("El documento ya se encuentra registrado.");
        }
        copiar(cliente, request.primerNombre(), request.segundoNombre(), request.primerApellido(),
                request.segundoApellido(), request.tipoDocumento(), documento, request.fechaNacimiento(),
                request.telefono(), request.correo());
        return clienteMapper.toDetalle(cliente, mascotaRepository.findByClienteId(id));
    }

    @Transactional
    public ClienteDetalleResponse cambiarEstado(Long id, CambiarEstadoClienteRequest request) {
        Cliente cliente = findCliente(id);
        cliente.setActivo(request.activo());
        return clienteMapper.toDetalle(cliente, mascotaRepository.findByClienteId(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MascotaResumenResponse> listarMascotas(Long id, String search, Boolean activo, Pageable pageable) {
        findCliente(id);
        validarPagina(pageable);
        Page<Mascota> page = mascotaRepository.findAllForGestion(normalizarNullable(search), activo, id, null, null, pageable);
        List<MascotaResumenResponse> content = page.getContent().stream()
                .map(mascota -> new MascotaResumenResponse(mascota.getId(), mascota.getCliente().getId(),
                        clienteMapper.nombreCompleto(mascota.getCliente()), mascota.getNombre(), mascota.getEspecie(),
                        mascota.getRaza(), mascota.getSexo(), mascota.getActivo())).toList();
        return new PaginaResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Cliente findCliente(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado."));
    }

    private void copiar(Cliente cliente, String primerNombre, String segundoNombre, String primerApellido,
            String segundoApellido, TipoDocumento tipoDocumento, String numeroDocumento, java.time.LocalDate fecha,
            String telefono, String correo) {
        cliente.setPrimerNombre(normalizar(primerNombre));
        cliente.setSegundoNombre(normalizarNullable(segundoNombre));
        cliente.setPrimerApellido(normalizar(primerApellido));
        cliente.setSegundoApellido(normalizarNullable(segundoApellido));
        cliente.setTipoDocumento(tipoDocumento);
        cliente.setNumeroDocumento(numeroDocumento);
        cliente.setFechaNacimiento(fecha);
        cliente.setTelefono(normalizarNullable(telefono));
        cliente.setCorreo(normalizarEmail(correo));
    }

    private String normalizar(String value) { return value.trim(); }
    private String normalizarNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizarEmail(String value) { return normalizarNullable(value) == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT); }
    private void validarPagina(Pageable pageable) { if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) throw new PageSizeClienteInvalidoException("El tamaño de pagina debe estar entre 1 y 100."); }
    private void validarOrden(Pageable pageable, String... allowed) { var values = java.util.Set.of(allowed); if (pageable.getSort().stream().anyMatch(order -> !values.contains(order.getProperty()))) throw new IllegalArgumentException("El orden solicitado no es valido."); }
}
