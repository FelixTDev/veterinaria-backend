package com.veterinaria.backend.reporte.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.reporte.dto.DashboardResumenResponse;
import com.veterinaria.backend.reporte.repository.ComprobanteReporteRepository;
import com.veterinaria.backend.reporte.repository.DashboardReporteRepository;

@Service
public class DashboardService {

    private final CitaReporteService citaReporteService;
    private final DashboardReporteRepository dashboardRepository;
    private final FinanzasReporteService finanzasReporteService;
    private final ComprobanteReporteRepository comprobanteRepository;

    public DashboardService(CitaReporteService citaReporteService, DashboardReporteRepository dashboardRepository,
            FinanzasReporteService finanzasReporteService, ComprobanteReporteRepository comprobanteRepository) {
        this.citaReporteService = citaReporteService;
        this.dashboardRepository = dashboardRepository;
        this.finanzasReporteService = finanzasReporteService;
        this.comprobanteRepository = comprobanteRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta) {
        var rango = ReporteRangoValidator.validar(desde, hasta);
        var citas = citaReporteService.resumen(rango.desde(), rango.hasta());
        BigDecimal ingresos = finanzasReporteService.resumen(rango.desde(), rango.hasta()).ingresoTotal();
        return new DashboardResumenResponse(citas,
                dashboardRepository.countAtencionesMedicas(rango.desde(), rango.hasta()),
                dashboardRepository.countAtencionesPeluqueria(rango.desde(), rango.hasta()),
                dashboardRepository.countClientesActivos(), dashboardRepository.countMascotasActivas(),
                dashboardRepository.countServiciosActivos(), ingresos,
                finanzasReporteService.saldoPendiente(rango.desde(), rango.hasta()),
                comprobanteRepository.emitidos(rango.desde(), rango.hasta()));
    }
}
