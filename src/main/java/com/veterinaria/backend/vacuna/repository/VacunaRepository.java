package com.veterinaria.backend.vacuna.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.vacuna.entity.Vacuna;

public interface VacunaRepository extends JpaRepository<Vacuna, Long> {
}
