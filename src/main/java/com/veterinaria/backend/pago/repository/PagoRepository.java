package com.veterinaria.backend.pago.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}
