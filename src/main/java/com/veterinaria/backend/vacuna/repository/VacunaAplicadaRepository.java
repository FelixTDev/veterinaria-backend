package com.veterinaria.backend.vacuna.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.vacuna.entity.VacunaAplicada;

public interface VacunaAplicadaRepository extends JpaRepository<VacunaAplicada, Long> {
}
