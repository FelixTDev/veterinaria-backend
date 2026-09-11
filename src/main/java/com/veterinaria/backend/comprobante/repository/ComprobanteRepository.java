package com.veterinaria.backend.comprobante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

import com.veterinaria.backend.comprobante.entity.Comprobante;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    boolean existsByPagoId(Long pagoId);
    @EntityGraph(attributePaths = {"pago", "pago.cita"})
    Optional<Comprobante> findWithPagoById(Long id);
    @EntityGraph(attributePaths = {"pago", "pago.cita"})
    Optional<Comprobante> findWithPagoByPagoId(Long pagoId);
}
