package com.veterinaria.backend.pago.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.pago.entity.DetallePago;

public interface DetallePagoRepository extends JpaRepository<DetallePago, Long> {
}
