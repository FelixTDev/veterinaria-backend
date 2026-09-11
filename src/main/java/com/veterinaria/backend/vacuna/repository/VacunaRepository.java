package com.veterinaria.backend.vacuna.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.vacuna.entity.Vacuna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VacunaRepository extends JpaRepository<Vacuna, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    Page<Vacuna> findByActivo(Boolean activo, Pageable pageable);
}
