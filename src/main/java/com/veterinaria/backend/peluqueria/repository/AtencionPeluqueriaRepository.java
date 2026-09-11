package com.veterinaria.backend.peluqueria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.peluqueria.entity.AtencionPeluqueria;

public interface AtencionPeluqueriaRepository extends JpaRepository<AtencionPeluqueria, Long> {

    boolean existsByCitaId(Long citaId);

    @EntityGraph(attributePaths = {"cita", "cita.trabajadorAsignado", "peluquero"})
    @Query("select atencion from AtencionPeluqueria atencion where atencion.id = :id")
    java.util.Optional<AtencionPeluqueria> findByIdWithCitaAndPeluquero(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cita", "peluquero"})
    @Query("select atencion from AtencionPeluqueria atencion where atencion.cita.id = :citaId")
    java.util.Optional<AtencionPeluqueria> findByCitaId(@Param("citaId") Long citaId);
}
