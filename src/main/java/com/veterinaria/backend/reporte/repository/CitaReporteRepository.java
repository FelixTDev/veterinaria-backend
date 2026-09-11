package com.veterinaria.backend.reporte.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.reporte.projection.EstadoCitaCountProjection;
import com.veterinaria.backend.reporte.projection.SerieTemporalProjection;
import com.veterinaria.backend.reporte.projection.TipoCitaCountProjection;

public interface CitaReporteRepository extends JpaRepository<Cita, Long> {

    @Query(value = "select estado as estado, count(*) as cantidad from citas where fecha_hora_inicio >= :desde and fecha_hora_inicio < :hasta group by estado order by estado", nativeQuery = true)
    List<EstadoCitaCountProjection> contarPorEstado(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select tipo_cita as tipo, count(*) as cantidad from citas where fecha_hora_inicio >= :desde and fecha_hora_inicio < :hasta group by tipo_cita order by tipo_cita", nativeQuery = true)
    List<TipoCitaCountProjection> contarPorTipo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select date_trunc('day', fecha_hora_inicio) as periodo, count(*) as cantidad from citas where fecha_hora_inicio >= :desde and fecha_hora_inicio < :hasta group by date_trunc('day', fecha_hora_inicio) order by periodo", nativeQuery = true)
    List<SerieTemporalProjection> tendenciaDiaria(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "select date_trunc('month', fecha_hora_inicio) as periodo, count(*) as cantidad from citas where fecha_hora_inicio >= :desde and fecha_hora_inicio < :hasta group by date_trunc('month', fecha_hora_inicio) order by periodo", nativeQuery = true)
    List<SerieTemporalProjection> tendenciaMensual(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
