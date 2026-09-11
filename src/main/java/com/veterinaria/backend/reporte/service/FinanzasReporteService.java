package com.veterinaria.backend.reporte.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.pago.enums.MedioPago;
import com.veterinaria.backend.reporte.dto.FinanzasResumenResponse;
import com.veterinaria.backend.reporte.dto.IngresoPorMedioResponse;
import com.veterinaria.backend.reporte.dto.SaldoPendienteResponse;
import com.veterinaria.backend.reporte.repository.FinanzasReporteRepository;
import com.veterinaria.backend.reporte.repository.SaldoReporteRepository;
import com.veterinaria.backend.usuario.dto.PaginaResponse;

@Service
public class FinanzasReporteService {

    private final FinanzasReporteRepository finanzasRepository;
    private final SaldoReporteRepository saldoRepository;

    public FinanzasReporteService(FinanzasReporteRepository finanzasRepository, SaldoReporteRepository saldoRepository) {
        this.finanzasRepository = finanzasRepository;
        this.saldoRepository = saldoRepository;
    }

    @Transactional(readOnly = true)
    public FinanzasResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        BigDecimal ingreso = finanzasRepository.ingresoTotal(rango.desde(), rango.hasta());
        Map<MedioPago, BigDecimal> porMedio = new EnumMap<>(MedioPago.class);
        Arrays.stream(MedioPago.values()).forEach(medio -> porMedio.put(medio, BigDecimal.ZERO));
        finanzasRepository.ingresoPorMedio(rango.desde(), rango.hasta()).forEach(item -> porMedio.put(MedioPago.valueOf(item.getMedioPago()), item.getMonto()));
        List<IngresoPorMedioResponse> items = Arrays.stream(MedioPago.values()).map(m -> new IngresoPorMedioResponse(m, porMedio.get(m))).toList();
        return new FinanzasResumenResponse(ingreso == null ? BigDecimal.ZERO : ingreso, finanzasRepository.cantidadPagos(rango.desde(), rango.hasta()), items);
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoPendiente(LocalDateTime desde, LocalDateTime hasta) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        BigDecimal saldo = saldoRepository.totalPendiente(rango.desde(), rango.hasta());
        return saldo == null ? BigDecimal.ZERO : saldo;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<SaldoPendienteResponse> saldosPendientes(LocalDateTime desde, LocalDateTime hasta, Integer page, Integer size) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        int pagina = page == null ? 0 : page;
        int tamanio = size == null ? 20 : size;
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new IllegalArgumentException("Los parametros de paginacion no son validos.");
        }
        var resultado = saldoRepository.pendientes(rango.desde(), rango.hasta(), PageRequest.of(pagina, tamanio));
        return new PaginaResponse<>(resultado.getContent().stream()
                .map(item -> new SaldoPendienteResponse(item.getCitaId(), item.getFechaHoraInicio(), item.getMascotaId(), item.getMascota(), item.getClienteId(), item.getCliente(), item.getTotalServicios(), item.getTotalPagado(), item.getSaldoPendiente()))
                .toList(), resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
    }
}
