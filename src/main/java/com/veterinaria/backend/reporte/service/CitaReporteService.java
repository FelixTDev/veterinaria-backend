package com.veterinaria.backend.reporte.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.reporte.dto.CitasResumenResponse;
import com.veterinaria.backend.reporte.dto.ConteoEstadoCitaResponse;
import com.veterinaria.backend.reporte.dto.ConteoTipoCitaResponse;
import com.veterinaria.backend.reporte.dto.GranularidadReporte;
import com.veterinaria.backend.reporte.dto.SerieTemporalResponse;
import com.veterinaria.backend.reporte.repository.CitaReporteRepository;

@Service
public class CitaReporteService {

    private final CitaReporteRepository repository;

    public CitaReporteService(CitaReporteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CitasResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        Map<EstadoCita, Long> estados = new EnumMap<>(EstadoCita.class);
        Arrays.stream(EstadoCita.values()).forEach(estado -> estados.put(estado, 0L));
        repository.contarPorEstado(rango.desde(), rango.hasta()).forEach(item -> estados.put(EstadoCita.valueOf(item.getEstado()), item.getCantidad()));
        Map<TipoCita, Long> tipos = new EnumMap<>(TipoCita.class);
        Arrays.stream(TipoCita.values()).forEach(tipo -> tipos.put(tipo, 0L));
        repository.contarPorTipo(rango.desde(), rango.hasta()).forEach(item -> tipos.put(TipoCita.valueOf(item.getTipo()), item.getCantidad()));
        List<ConteoEstadoCitaResponse> porEstado = Arrays.stream(EstadoCita.values()).map(e -> new ConteoEstadoCitaResponse(e, estados.get(e))).toList();
        List<ConteoTipoCitaResponse> porTipo = Arrays.stream(TipoCita.values()).map(t -> new ConteoTipoCitaResponse(t, tipos.get(t))).toList();
        return new CitasResumenResponse(estados.values().stream().mapToLong(Long::longValue).sum(), porEstado, porTipo);
    }

    @Transactional(readOnly = true)
    public List<SerieTemporalResponse> tendencia(LocalDateTime desde, LocalDateTime hasta, GranularidadReporte granularidad) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        if (granularidad == null) {
            throw new IllegalArgumentException("La granularidad es obligatoria.");
        }
        var resultado = granularidad == GranularidadReporte.DIARIA
                ? repository.tendenciaDiaria(rango.desde(), rango.hasta())
                : repository.tendenciaMensual(rango.desde(), rango.hasta());
        return resultado.stream()
                .map(item -> new SerieTemporalResponse(item.getPeriodo(), item.getCantidad()))
                .toList();
    }
}
