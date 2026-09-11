package com.veterinaria.backend.peluqueria.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.exception.CitaNoEncontradaException;
import com.veterinaria.backend.peluqueria.dto.AtencionPeluqueriaResponse;
import com.veterinaria.backend.peluqueria.dto.FotoPeluqueriaResponse;
import com.veterinaria.backend.peluqueria.entity.AtencionPeluqueria;
import com.veterinaria.backend.peluqueria.entity.FotoPeluqueria;
import com.veterinaria.backend.peluqueria.enums.TipoFoto;
import com.veterinaria.backend.peluqueria.exception.AtencionPeluqueriaNoEncontradaException;
import com.veterinaria.backend.peluqueria.exception.EvidenciaInvalidaException;
import com.veterinaria.backend.peluqueria.exception.PeluqueriaConflictException;
import com.veterinaria.backend.peluqueria.repository.AtencionPeluqueriaRepository;
import com.veterinaria.backend.peluqueria.repository.FotoPeluqueriaRepository;
import com.veterinaria.backend.peluqueria.storage.ImageStorageService;
import com.veterinaria.backend.peluqueria.storage.StoredImage;
import com.veterinaria.backend.usuario.enums.NombreRol;

@Service
public class AtencionPeluqueriaService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AtencionPeluqueriaRepository atencionRepository;
    private final FotoPeluqueriaRepository fotoRepository;
    private final CitaRepository citaRepository;
    private final ImageStorageService imageStorageService;

    public AtencionPeluqueriaService(
            AtencionPeluqueriaRepository atencionRepository,
            FotoPeluqueriaRepository fotoRepository,
            CitaRepository citaRepository,
            ImageStorageService imageStorageService) {
        this.atencionRepository = atencionRepository;
        this.fotoRepository = fotoRepository;
        this.citaRepository = citaRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public AtencionPeluqueriaResponse crear(Long citaId, Long actorId, Set<NombreRol> roles, String observaciones) {
        validarRolPeluquero(roles);
        Cita cita = citaRepository.findByIdForUpdate(citaId)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada."));
        validarCitaParaNuevaAtencion(cita);
        validarActorAsignado(cita, actorId);
        if (atencionRepository.existsByCitaId(citaId)) {
            throw new PeluqueriaConflictException("La cita ya tiene una atencion de peluqueria.");
        }

        AtencionPeluqueria atencion = new AtencionPeluqueria();
        atencion.setCita(cita);
        atencion.setPeluquero(cita.getTrabajadorAsignado());
        atencion.setObservaciones(normalizarTexto(observaciones));
        try {
            AtencionPeluqueria saved = atencionRepository.saveAndFlush(atencion);
            return toResponse(saved, List.of());
        } catch (DataIntegrityViolationException exception) {
            throw new PeluqueriaConflictException("La cita ya tiene una atencion de peluqueria.");
        }
    }

    @Transactional(readOnly = true)
    public AtencionPeluqueriaResponse obtenerPorCita(Long citaId, Long actorId, Set<NombreRol> roles) {
        AtencionPeluqueria atencion = atencionRepository.findByCitaId(citaId)
                .orElseThrow(() -> new AtencionPeluqueriaNoEncontradaException("Atencion de peluqueria no encontrada."));
        validarConsulta(atencion, actorId, roles);
        return toResponse(atencion, cargarFotos(atencion.getId()));
    }

    @Transactional(readOnly = true)
    public List<FotoPeluqueriaResponse> listarEvidencias(Long atencionId, Long actorId, Set<NombreRol> roles) {
        AtencionPeluqueria atencion = obtenerAtencion(atencionId);
        validarConsulta(atencion, actorId, roles);
        return cargarFotos(atencionId);
    }

    @Transactional
    public FotoPeluqueriaResponse agregarEvidencia(
            Long atencionId,
            Long actorId,
            Set<NombreRol> roles,
            MultipartFile archivo,
            TipoFoto tipoFoto) {
        validarRolPeluquero(roles);
        AtencionPeluqueria atencion = obtenerAtencion(atencionId);
        validarActorAsignado(atencion.getCita(), actorId);
        validarAtencionAbierta(atencion);
        validarArchivo(archivo, tipoFoto);

        String folder = "veterinaria/peluqueria/" + atencionId + "/" + tipoFoto.name().toLowerCase();
        String generatedName = UUID.randomUUID().toString();
        StoredImage stored = imageStorageService.upload(archivo, folder, generatedName);
        FotoPeluqueria foto = new FotoPeluqueria();
        foto.setAtencionPeluqueria(atencion);
        foto.setTipoFoto(tipoFoto);
        foto.setUrlArchivo(stored.secureUrl());
        foto.setStorageKey(stored.storageKey());
        foto.setNombreArchivo(nombreSeguro(archivo.getOriginalFilename()));
        try {
            return toFotoResponse(fotoRepository.saveAndFlush(foto));
        } catch (RuntimeException exception) {
            try {
                imageStorageService.delete(stored.storageKey());
            } catch (RuntimeException compensationException) {
                // El fallo de compensacion se conserva en la causa original y se registra para operacion posterior.
                org.slf4j.LoggerFactory.getLogger(getClass()).error(
                        "No se pudo compensar asset de Cloudinary storageKey={}", stored.storageKey(), compensationException);
            }
            throw exception;
        }
    }

    @Transactional
    public AtencionPeluqueriaResponse cerrar(Long atencionId, Long actorId, Set<NombreRol> roles) {
        validarRolPeluquero(roles);
        AtencionPeluqueria atencion = obtenerAtencion(atencionId);
        Cita cita = citaRepository.findByIdForUpdate(atencion.getCita().getId())
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada."));
        validarCitaParaCierre(cita);
        validarActorAsignado(cita, actorId);
        if (!fotoRepository.existsByAtencionPeluqueriaId(atencionId)) {
            throw new PeluqueriaConflictException("La atencion requiere al menos una evidencia.");
        }
        cita.setEstado(EstadoCita.ATENDIDA);
        return toResponse(atencion, cargarFotos(atencionId));
    }

    private AtencionPeluqueria obtenerAtencion(Long id) {
        return atencionRepository.findByIdWithCitaAndPeluquero(id)
                .orElseThrow(() -> new AtencionPeluqueriaNoEncontradaException("Atencion de peluqueria no encontrada."));
    }

    private List<FotoPeluqueriaResponse> cargarFotos(Long atencionId) {
        return fotoRepository.findAllByAtencionPeluqueriaIdOrderByCreatedAtAscIdAsc(atencionId).stream()
                .map(this::toFotoResponse)
                .toList();
    }

    private AtencionPeluqueriaResponse toResponse(AtencionPeluqueria atencion, List<FotoPeluqueriaResponse> fotos) {
        return new AtencionPeluqueriaResponse(
                atencion.getId(),
                atencion.getCita().getId(),
                atencion.getPeluquero().getId(),
                atencion.getObservaciones(),
                atencion.getFechaAtencion(),
                atencion.getCita().getEstado(),
                fotos);
    }

    private FotoPeluqueriaResponse toFotoResponse(FotoPeluqueria foto) {
        return new FotoPeluqueriaResponse(
                foto.getId(), foto.getTipoFoto(), foto.getUrlArchivo(), foto.getNombreArchivo(), foto.getCreatedAt());
    }

    private void validarCitaParaNuevaAtencion(Cita cita) {
        if (cita.getTipoCita() != TipoCita.PELUQUERIA || cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new PeluqueriaConflictException("La cita debe ser de peluqueria y estar confirmada.");
        }
    }

    private void validarCitaParaCierre(Cita cita) {
        if (cita.getTipoCita() != TipoCita.PELUQUERIA || cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new PeluqueriaConflictException("La cita debe ser de peluqueria y estar confirmada para cerrarse.");
        }
    }

    private void validarAtencionAbierta(AtencionPeluqueria atencion) {
        if (atencion.getCita().getEstado() != EstadoCita.CONFIRMADA) {
            throw new PeluqueriaConflictException("La atencion ya no admite nuevas evidencias.");
        }
    }

    private void validarActorAsignado(Cita cita, Long actorId) {
        if (actorId == null || cita.getTrabajadorAsignado() == null
                || !actorId.equals(cita.getTrabajadorAsignado().getId())
                || !Boolean.TRUE.equals(cita.getTrabajadorAsignado().getActivo())) {
            throw new AccessDeniedException("Acceso denegado.");
        }
    }

    private void validarConsulta(AtencionPeluqueria atencion, Long actorId, Set<NombreRol> roles) {
        if (roles != null && roles.contains(NombreRol.ADMINISTRADOR)) {
            return;
        }
        validarRolPeluquero(roles);
        validarActorAsignado(atencion.getCita(), actorId);
    }

    private void validarRolPeluquero(Set<NombreRol> roles) {
        if (roles == null || !roles.contains(NombreRol.PELUQUERO)) {
            throw new AccessDeniedException("Acceso denegado.");
        }
    }

    private void validarArchivo(MultipartFile archivo, TipoFoto tipoFoto) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_FILE_SIZE || tipoFoto == null) {
            throw new EvidenciaInvalidaException("La evidencia no es valida.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new EvidenciaInvalidaException("El formato de imagen no es valido.");
        }
        try {
            byte[] bytes = archivo.getBytes();
            if (!firmaCompatible(bytes, contentType.toLowerCase())) {
                throw new EvidenciaInvalidaException("El contenido no corresponde al formato declarado.");
            }
        } catch (IOException exception) {
            throw new EvidenciaInvalidaException("No se pudo leer la evidencia.");
        }
    }

    private boolean firmaCompatible(byte[] bytes, String contentType) {
        if (contentType.equals("image/jpeg")) {
            return bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        }
        if (contentType.equals("image/png")) {
            byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            return startsWith(bytes, signature);
        }
        byte[] riff = {'R', 'I', 'F', 'F'};
        byte[] webp = {'W', 'E', 'B', 'P'};
        return bytes.length >= 12 && startsWith(bytes, riff) && startsWith(bytes, webp, 8);
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return startsWith(value, prefix, 0);
    }

    private boolean startsWith(byte[] value, byte[] prefix, int offset) {
        if (value.length < offset + prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[offset + i] != prefix[i]) return false;
        return true;
    }

    private String nombreSeguro(String original) {
        if (original == null || original.isBlank()) return null;
        String normalized = original.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return basename.length() <= 255 ? basename : basename.substring(0, 255);
    }

    private String normalizarTexto(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
