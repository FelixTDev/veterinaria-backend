package com.veterinaria.backend.cita.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.cita.entity.CitaServicio;

public interface CitaServicioRepository extends JpaRepository<CitaServicio, Long> {
}
