package com.veterinaria.backend.atencionmedica.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {
}
