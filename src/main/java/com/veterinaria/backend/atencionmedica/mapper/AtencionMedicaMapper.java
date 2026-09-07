package com.veterinaria.backend.atencionmedica.mapper;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResponse;
import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResumenResponse;
import com.veterinaria.backend.atencionmedica.dto.VeterinarioResumenResponse;
import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.usuario.entity.Usuario;

@Component
public class AtencionMedicaMapper {

    public AtencionMedicaResponse toResponse(AtencionMedica atencion) {
        return new AtencionMedicaResponse(
                atencion.getId(), atencion.getCita().getId(), atencion.getCita().getMascota().getId(),
                atencion.getCita().getMascota().getNombre(), veterinario(atencion.getVeterinario()),
                atencion.getPesoKg(), atencion.getTemperaturaC(), trim(atencion.getSintomas()),
                trim(atencion.getDiagnostico()), trim(atencion.getMotivoSinDiagnostico()),
                trim(atencion.getTratamiento()), trim(atencion.getMotivoSinTratamiento()),
                trim(atencion.getReceta()), trim(atencion.getMotivoSinReceta()), trim(atencion.getObservaciones()),
                atencion.getFechaAtencion(), atencion.getCreatedAt(), atencion.getUpdatedAt());
    }

    public AtencionMedicaResumenResponse toResumen(AtencionMedica atencion) {
        String diagnostico = trim(atencion.getDiagnostico());
        if (diagnostico == null) {
            diagnostico = trim(atencion.getMotivoSinDiagnostico());
        }
        return new AtencionMedicaResumenResponse(
                atencion.getId(), atencion.getCita().getId(), atencion.getFechaAtencion(),
                veterinario(atencion.getVeterinario()), diagnostico);
    }

    private VeterinarioResumenResponse veterinario(Usuario usuario) {
        return new VeterinarioResumenResponse(usuario.getId(), nombreCompleto(usuario), usuario.getCorreo());
    }

    private String nombreCompleto(Usuario usuario) {
        return Stream.of(usuario.getPrimerNombre(), usuario.getSegundoNombre(),
                        usuario.getPrimerApellido(), usuario.getSegundoApellido())
                .map(this::trim).filter(value -> value != null).reduce((a, b) -> a + " " + b).orElse("");
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
