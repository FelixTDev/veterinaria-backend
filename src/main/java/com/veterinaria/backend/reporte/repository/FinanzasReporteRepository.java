package com.veterinaria.backend.reporte.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.pago.entity.Pago;
import com.veterinaria.backend.reporte.projection.IngresoPorMedioProjection;

public interface FinanzasReporteRepository extends JpaRepository<Pago, Long> {

    @Query("select coalesce(sum(p.montoTotal), 0) from Pago p where p.estado = com.veterinaria.backend.pago.enums.EstadoPago.PAGADO and p.fechaPago >= :desde and p.fechaPago < :hasta")
    BigDecimal ingresoTotal(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(p) from Pago p where p.estado = com.veterinaria.backend.pago.enums.EstadoPago.PAGADO and p.fechaPago >= :desde and p.fechaPago < :hasta")
    long cantidadPagos(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select d.medio_pago as medio_pago, coalesce(sum(d.monto), 0) as monto from detalle_pagos d join pagos p on p.id = d.pago_id where p.estado = 'PAGADO' and p.fecha_pago >= :desde and p.fecha_pago < :hasta group by d.medio_pago order by d.medio_pago", nativeQuery = true)
    List<IngresoPorMedioProjection> ingresoPorMedio(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
