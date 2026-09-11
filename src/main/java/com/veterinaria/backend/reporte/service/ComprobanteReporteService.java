package com.veterinaria.backend.reporte.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.reporte.dto.ComprobantesResumenResponse;
import com.veterinaria.backend.reporte.repository.ComprobanteReporteRepository;

@Service
public class ComprobanteReporteService {

    private final ComprobanteReporteRepository repository;

    public ComprobanteReporteService(ComprobanteReporteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ComprobantesResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        return new ComprobantesResumenResponse(repository.emitidos(rango.desde(), rango.hasta()),
                repository.boletas(rango.desde(), rango.hasta()), repository.facturas(rango.desde(), rango.hasta()));
    }
}
