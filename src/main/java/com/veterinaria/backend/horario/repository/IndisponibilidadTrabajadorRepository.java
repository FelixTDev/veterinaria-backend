package com.veterinaria.backend.horario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.horario.entity.IndisponibilidadTrabajador;

public interface IndisponibilidadTrabajadorRepository extends JpaRepository<IndisponibilidadTrabajador, Long> {
}
