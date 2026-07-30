package com.veterinaria.backend.mascota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.mascota.entity.Mascota;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByClienteId(Long clienteId);
}
