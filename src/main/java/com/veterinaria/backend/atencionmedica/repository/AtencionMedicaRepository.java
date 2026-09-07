package com.veterinaria.backend.atencionmedica.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {

    @EntityGraph(attributePaths = { "cita", "cita.mascota", "veterinario" })
    Optional<AtencionMedica> findByCitaId(Long citaId);

    @EntityGraph(attributePaths = { "cita", "cita.mascota", "veterinario" })
    @Query("select atencion from AtencionMedica atencion where atencion.cita.mascota.id = :mascotaId order by atencion.fechaAtencion desc, atencion.id desc")
    Page<AtencionMedica> findHistorial(@Param("mascotaId") Long mascotaId, Pageable pageable);

    @EntityGraph(attributePaths = { "cita", "cita.mascota", "veterinario" })
    @Query("select atencion from AtencionMedica atencion where atencion.cita.mascota.id = :mascotaId and atencion.fechaAtencion >= :desde order by atencion.fechaAtencion desc, atencion.id desc")
    Page<AtencionMedica> findHistorialDesde(@Param("mascotaId") Long mascotaId, @Param("desde") java.time.LocalDateTime desde, Pageable pageable);

    @EntityGraph(attributePaths = { "cita", "cita.mascota", "veterinario" })
    @Query("select atencion from AtencionMedica atencion where atencion.cita.mascota.id = :mascotaId and atencion.fechaAtencion < :hasta order by atencion.fechaAtencion desc, atencion.id desc")
    Page<AtencionMedica> findHistorialHasta(@Param("mascotaId") Long mascotaId, @Param("hasta") java.time.LocalDateTime hasta, Pageable pageable);

    @EntityGraph(attributePaths = { "cita", "cita.mascota", "veterinario" })
    @Query("select atencion from AtencionMedica atencion where atencion.cita.mascota.id = :mascotaId and atencion.fechaAtencion >= :desde and atencion.fechaAtencion < :hasta order by atencion.fechaAtencion desc, atencion.id desc")
    Page<AtencionMedica> findHistorialEntre(@Param("mascotaId") Long mascotaId, @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta, Pageable pageable);
}
