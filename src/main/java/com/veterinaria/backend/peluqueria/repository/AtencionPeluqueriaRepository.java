package com.veterinaria.backend.peluqueria.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.peluqueria.entity.AtencionPeluqueria;

public interface AtencionPeluqueriaRepository extends JpaRepository<AtencionPeluqueria, Long> {
}
