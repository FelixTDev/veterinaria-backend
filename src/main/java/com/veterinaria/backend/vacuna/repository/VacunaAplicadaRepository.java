package com.veterinaria.backend.vacuna.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.vacuna.entity.VacunaAplicada;

public interface VacunaAplicadaRepository extends JpaRepository<VacunaAplicada, Long> {
    @EntityGraph(attributePaths = {"vacuna", "atencionMedica", "atencionMedica.cita", "atencionMedica.cita.mascota"})
    List<VacunaAplicada> findByAtencionMedicaIdOrderByFechaAplicacionDescIdDesc(Long atencionId);

    @Query("""
            select a from VacunaAplicada a
            join fetch a.vacuna v
            join fetch a.atencionMedica am
            join fetch am.cita c
            join fetch c.mascota m
            where m.id = :mascotaId
              and (:desde is null or a.fechaAplicacion >= :desde)
              and (:hasta is null or a.fechaAplicacion <= :hasta)
              and (:vacunaId is null or v.id = :vacunaId)
            order by a.fechaAplicacion desc, a.id desc
            """)
    Page<VacunaAplicada> findHistorial(@Param("mascotaId") Long mascotaId, @Param("desde") java.time.LocalDate desde, @Param("hasta") java.time.LocalDate hasta, @Param("vacunaId") Long vacunaId, Pageable pageable);
}
