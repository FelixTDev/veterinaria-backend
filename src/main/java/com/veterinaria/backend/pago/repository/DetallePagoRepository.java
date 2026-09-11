package com.veterinaria.backend.pago.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.pago.entity.DetallePago;

public interface DetallePagoRepository extends JpaRepository<DetallePago, Long> {

    @EntityGraph(attributePaths = {"pago"})
    @org.springframework.data.jpa.repository.Query("select d from DetallePago d where d.pago.id in :pagoIds order by d.pago.id asc, d.id asc")
    List<DetallePago> findAllByPagoIdIn(@Param("pagoIds") Collection<Long> pagoIds);

    @EntityGraph(attributePaths = {"pago"})
    List<DetallePago> findAllByPagoIdOrderByIdAsc(Long pagoId);
}
