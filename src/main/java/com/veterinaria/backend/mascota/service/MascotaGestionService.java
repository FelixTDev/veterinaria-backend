package com.veterinaria.backend.mascota.service;

import java.time.LocalDate;
import java.util.List;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.exception.ClienteNoEncontradoException;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.dto.ActualizarMascotaRequest;
import com.veterinaria.backend.mascota.dto.CambiarEstadoMascotaRequest;
import com.veterinaria.backend.mascota.dto.CrearMascotaRequest;
import com.veterinaria.backend.mascota.dto.MascotaDetalleResponse;
import com.veterinaria.backend.mascota.dto.MascotaResumenResponse;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;
import com.veterinaria.backend.mascota.exception.ClienteMascotaInactivoException;
import com.veterinaria.backend.mascota.exception.MascotaNoEncontradaException;
import com.veterinaria.backend.mascota.exception.PageSizeMascotaInvalidoException;
import com.veterinaria.backend.mascota.mapper.MascotaMapper;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MascotaGestionService {

    private final MascotaRepository mascotaRepository;
    private final ClienteRepository clienteRepository;
    private final MascotaMapper mascotaMapper;

    public MascotaGestionService(MascotaRepository mascotaRepository, ClienteRepository clienteRepository,
            MascotaMapper mascotaMapper) {
        this.mascotaRepository = mascotaRepository;
        this.clienteRepository = clienteRepository;
        this.mascotaMapper = mascotaMapper;
    }

    @Transactional
    public MascotaDetalleResponse crear(CrearMascotaRequest request) {
        Cliente cliente = findCliente(request.clienteId());
        requireActiveOwner(cliente);
        validarFecha(request.fechaNacimiento());
        Mascota mascota = new Mascota();
        mascota.setCliente(cliente);
        copiar(mascota, request.nombre(), request.especie(), request.raza(), request.color(), request.sexo(),
                request.pesoKg(), request.fechaNacimiento(), request.edadAproximadaAnios());
        mascota.setActivo(Boolean.TRUE);
        return mascotaMapper.toDetalle(mascotaRepository.save(mascota));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MascotaResumenResponse> listar(String search, Boolean activo, Long clienteId,
            EspecieMascota especie, SexoMascota sexo, Pageable pageable) {
        validarPagina(pageable);
        validarOrden(pageable, "id", "nombre", "especie", "sexo", "activo");
        Page<Mascota> page = mascotaRepository.findAllForGestion(normalizarNullable(search), activo, clienteId, especie, sexo, pageable);
        return pagina(page);
    }

    @Transactional(readOnly = true)
    public MascotaDetalleResponse obtener(Long id) {
        return mascotaMapper.toDetalle(findMascota(id));
    }

    @Transactional
    public MascotaDetalleResponse actualizar(Long id, ActualizarMascotaRequest request) {
        Mascota mascota = findMascota(id);
        validarFecha(request.fechaNacimiento());
        copiar(mascota, request.nombre(), request.especie(), request.raza(), request.color(), request.sexo(),
                request.pesoKg(), request.fechaNacimiento(), request.edadAproximadaAnios());
        return mascotaMapper.toDetalle(mascota);
    }

    @Transactional
    public MascotaDetalleResponse cambiarEstado(Long id, CambiarEstadoMascotaRequest request) {
        Mascota mascota = findMascota(id);
        mascota.setActivo(request.activo());
        return mascotaMapper.toDetalle(mascota);
    }

    private PaginaResponse<MascotaResumenResponse> pagina(Page<Mascota> page) {
        List<MascotaResumenResponse> content = page.getContent().stream().map(mascotaMapper::toResumen).toList();
        return new PaginaResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Cliente findCliente(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado."));
    }

    private Mascota findMascota(Long id) {
        return mascotaRepository.findById(id).orElseThrow(() -> new MascotaNoEncontradaException("Mascota no encontrada."));
    }

    private void requireActiveOwner(Cliente cliente) {
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteMascotaInactivoException("El cliente propietario no esta activo.");
        }
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha != null && fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
    }

    private void copiar(Mascota mascota, String nombre, EspecieMascota especie, String raza, String color,
            SexoMascota sexo, java.math.BigDecimal pesoKg, LocalDate fecha, Integer edad) {
        if (pesoKg != null && pesoKg.signum() <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor que cero.");
        }
        if (edad != null && edad < 0) {
            throw new IllegalArgumentException("La edad aproximada no puede ser negativa.");
        }
        mascota.setNombre(nombre.trim());
        mascota.setEspecie(especie);
        mascota.setRaza(normalizarNullable(raza));
        mascota.setColor(normalizarNullable(color));
        mascota.setSexo(sexo);
        mascota.setPesoKg(pesoKg);
        mascota.setFechaNacimiento(fecha);
        mascota.setEdadAproximadaAnios(edad);
    }

    private String normalizarNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void validarPagina(Pageable pageable) { if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) throw new PageSizeMascotaInvalidoException("El tamaño de pagina debe estar entre 1 y 100."); }
    private void validarOrden(Pageable pageable, String... allowed) { var values = java.util.Set.of(allowed); if (pageable.getSort().stream().anyMatch(order -> !values.contains(order.getProperty()))) throw new IllegalArgumentException("El orden solicitado no es valido."); }
}
