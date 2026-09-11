package com.veterinaria.backend.reporte.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.reporte.projection.VacunaRankingProjection;
import com.veterinaria.backend.vacuna.entity.VacunaAplicada;

public interface VacunaReporteRepository extends JpaRepository<VacunaAplicada, Long> {

    @Query("select count(a) from VacunaAplicada a where a.fechaAplicacion >= :desde and a.fechaAplicacion < :hasta")
    long totalAplicadas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = "select v.id as vacuna_id, v.nombre as nombre, count(a.id) as cantidad from vacunas_aplicadas a join vacunas v on v.id = a.vacuna_id where a.fecha_aplicacion >= :desde and a.fecha_aplicacion < :hasta group by v.id, v.nombre order by cantidad desc, v.id asc limit 100", nativeQuery = true)
    List<VacunaRankingProjection> masAplicadas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
