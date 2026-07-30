package com.veterinaria.backend.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.auth.entity.CodigoRecuperacion;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    List<CodigoRecuperacion> findByUsuario_IdAndUsadoFalse(Long usuarioId);

    Optional<CodigoRecuperacion> findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(Long usuarioId);

    Optional<CodigoRecuperacion> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
