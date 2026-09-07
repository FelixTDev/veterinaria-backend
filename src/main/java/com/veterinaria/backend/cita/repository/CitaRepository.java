package com.veterinaria.backend.cita.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;

import jakarta.persistence.LockModeType;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    List<Cita> findByTrabajadorAsignadoIdAndFechaHoraInicioBetween(Long trabajadorId, LocalDateTime desde, LocalDateTime hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cita
            from Cita cita
            where cita.id = :id
            """)
    Optional<Cita> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(cita) > 0
            from Cita cita
            where cita.trabajadorAsignado.id = :trabajadorId
              and cita.estado in :estados
              and cita.fechaHoraInicio < :fin
              and cita.fechaHoraFin > :inicio
              and (:excluirId is null or cita.id <> :excluirId)
            """)
    boolean existsSolapamiento(
            @Param("trabajadorId") Long trabajadorId,
            @Param("estados") Set<EstadoCita> estados,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("excluirId") Long excluirId);

    @Override
    @EntityGraph(attributePaths = { "mascota", "mascota.cliente", "trabajadorAsignado", "registradoPor" })
    Page<Cita> findAll(org.springframework.data.jpa.domain.Specification<Cita> specification, Pageable pageable);
}
