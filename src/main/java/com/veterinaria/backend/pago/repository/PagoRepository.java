package com.veterinaria.backend.pago.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("select coalesce(sum(p.montoTotal), 0) from Pago p where p.cita.id = :citaId and p.estado = com.veterinaria.backend.pago.enums.EstadoPago.PAGADO")
    BigDecimal sumPagadoByCitaId(@Param("citaId") Long citaId);

    @EntityGraph(attributePaths = {"cita", "registradoPor"})
    Page<Pago> findAllByCitaId(Long citaId, Pageable pageable);

    @EntityGraph(attributePaths = {"cita", "registradoPor"})
    Optional<Pago> findWithCitaAndRegistradoPorById(Long id);

    @EntityGraph(attributePaths = {"cita", "registradoPor"})
    List<Pago> findAllByCitaIdOrderByCreatedAtAscIdAsc(Long citaId);
}
