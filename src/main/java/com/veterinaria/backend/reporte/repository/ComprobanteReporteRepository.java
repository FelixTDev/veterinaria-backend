package com.veterinaria.backend.reporte.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.comprobante.entity.Comprobante;

public interface ComprobanteReporteRepository extends JpaRepository<Comprobante, Long> {

    @Query("select count(c) from Comprobante c where c.estado = com.veterinaria.backend.comprobante.enums.EstadoComprobante.EMITIDO and c.fechaEmision >= :desde and c.fechaEmision < :hasta")
    long emitidos(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(c) from Comprobante c where c.estado = com.veterinaria.backend.comprobante.enums.EstadoComprobante.EMITIDO and c.tipoComprobante = com.veterinaria.backend.comprobante.enums.TipoComprobante.BOLETA and c.fechaEmision >= :desde and c.fechaEmision < :hasta")
    long boletas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(c) from Comprobante c where c.estado = com.veterinaria.backend.comprobante.enums.EstadoComprobante.EMITIDO and c.tipoComprobante = com.veterinaria.backend.comprobante.enums.TipoComprobante.FACTURA and c.fechaEmision >= :desde and c.fechaEmision < :hasta")
    long facturas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
