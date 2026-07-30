package com.veterinaria.backend.servicio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;

public interface PrecioServicioTamanoRepository extends JpaRepository<PrecioServicioTamano, Long> {

    List<PrecioServicioTamano> findByServicioNombre(String nombreServicio);
}
