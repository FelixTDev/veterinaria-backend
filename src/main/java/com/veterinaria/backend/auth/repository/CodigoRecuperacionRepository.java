package com.veterinaria.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.auth.entity.CodigoRecuperacion;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {
}
