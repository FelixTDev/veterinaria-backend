package com.veterinaria.backend.cita.repository;

import java.util.Collection;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cita.entity.CitaServicio;

public interface CitaServicioRepository extends JpaRepository<CitaServicio, Long> {

    @Query("select coalesce(sum(cs.precioAplicado), 0) from CitaServicio cs where cs.cita.id = :citaId")
    BigDecimal sumPrecioAplicadoByCitaId(@Param("citaId") Long citaId);

    @EntityGraph(attributePaths = { "servicio", "precioServicioTamano" })
    @Query("""
            select citaServicio
            from CitaServicio citaServicio
            where citaServicio.cita.id in :citaIds
            order by citaServicio.cita.id asc, citaServicio.id asc
            """)
    List<CitaServicio> findByCitaIdIn(@Param("citaIds") Collection<Long> citaIds);
}
