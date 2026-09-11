package com.veterinaria.backend.reporte.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.reporte.projection.ServicioRankingProjection;

public interface ServicioReporteRepository extends JpaRepository<CitaServicio, Long> {

    @Query(value = "select s.id as servicio_id, s.nombre as nombre, s.tipo_servicio as tipo_servicio, count(cs.id) as cantidad from cita_servicios cs join citas c on c.id = cs.cita_id join servicios s on s.id = cs.servicio_id where c.estado = 'ATENDIDA' and c.fecha_hora_inicio >= :desde and c.fecha_hora_inicio < :hasta group by s.id, s.nombre, s.tipo_servicio order by cantidad desc, s.id asc limit :limite", nativeQuery = true)
    List<ServicioRankingProjection> masSolicitados(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("limite") int limite);
}
