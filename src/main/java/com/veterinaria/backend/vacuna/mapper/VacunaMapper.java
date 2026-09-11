package com.veterinaria.backend.vacuna.mapper;

import org.springframework.stereotype.Component;
import com.veterinaria.backend.vacuna.dto.*;
import com.veterinaria.backend.vacuna.entity.*;

@Component
public class VacunaMapper {
    public VacunaResponse toResponse(Vacuna v) { return new VacunaResponse(v.getId(), v.getNombre(), v.getDescripcion(), v.getActivo(), v.getCreatedAt(), v.getUpdatedAt()); }
    public VacunaResumenResponse toResumen(Vacuna v) { return new VacunaResumenResponse(v.getId(), v.getNombre(), v.getActivo()); }
    public VacunaAplicadaResponse toAplicadaResponse(VacunaAplicada a) { return new VacunaAplicadaResponse(a.getId(), a.getAtencionMedica().getId(), a.getAtencionMedica().getCita().getMascota().getId(), toResumen(a.getVacuna()), a.getFechaAplicacion(), a.getProximaFecha(), a.getLote(), a.getObservaciones()); }
    public VacunaAplicadaResumenResponse toAplicadaResumen(VacunaAplicada a) { return new VacunaAplicadaResumenResponse(a.getId(), a.getAtencionMedica().getId(), toResumen(a.getVacuna()), a.getFechaAplicacion(), a.getProximaFecha(), a.getLote(), a.getObservaciones()); }
}
