package com.veterinaria.backend.reporte.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.reporte.dto.VacunaRankingResponse;
import com.veterinaria.backend.reporte.dto.VacunasResumenResponse;
import com.veterinaria.backend.reporte.repository.VacunaReporteRepository;

@Service
public class VacunaReporteService {

    private final VacunaReporteRepository repository;

    public VacunaReporteService(VacunaReporteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public VacunasResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        var desdeFecha = rango.desde().toLocalDate();
        var hastaFecha = rango.hasta().toLocalDate();
        List<VacunaRankingResponse> ranking = repository.masAplicadas(desdeFecha, hastaFecha).stream()
                .map(item -> new VacunaRankingResponse(item.getVacunaId(), item.getNombre(), item.getCantidad()))
                .toList();
        return new VacunasResumenResponse(repository.totalAplicadas(desdeFecha, hastaFecha), ranking);
    }
}
