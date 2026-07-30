package com.veterinaria.backend.cita.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.cita.entity.Cita;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByTrabajadorAsignadoIdAndFechaHoraInicioBetween(Long trabajadorId, LocalDateTime desde, LocalDateTime hasta);
}
