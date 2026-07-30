package com.veterinaria.backend.comprobante.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.comprobante.entity.Comprobante;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
}
