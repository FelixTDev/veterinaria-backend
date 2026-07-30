package com.veterinaria.backend.horario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.horario.entity.HorarioTrabajador;

public interface HorarioTrabajadorRepository extends JpaRepository<HorarioTrabajador, Long> {
}
