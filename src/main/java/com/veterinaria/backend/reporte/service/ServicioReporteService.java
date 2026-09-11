package com.veterinaria.backend.reporte.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.reporte.dto.ServicioRankingResponse;
import com.veterinaria.backend.reporte.repository.ServicioReporteRepository;
import com.veterinaria.backend.servicio.enums.TipoServicio;

@Service
public class ServicioReporteService {

    private final ServicioReporteRepository repository;

    public ServicioReporteService(ServicioReporteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServicioRankingResponse> masSolicitados(LocalDateTime desde, LocalDateTime hasta, Integer limit) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        int limite = limit == null ? 10 : limit;
        if (limite < 1 || limite > 100) {
            throw new IllegalArgumentException("El limite debe estar entre 1 y 100.");
        }
        return repository.masSolicitados(rango.desde(), rango.hasta(), limite).stream()
                .map(item -> new ServicioRankingResponse(item.getServicioId(), item.getNombre(), TipoServicio.valueOf(item.getTipoServicio()), item.getCantidad()))
                .toList();
    }
}
