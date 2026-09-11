package com.veterinaria.backend.peluqueria.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.peluqueria.entity.FotoPeluqueria;

public interface FotoPeluqueriaRepository extends JpaRepository<FotoPeluqueria, Long> {

    boolean existsByAtencionPeluqueriaId(Long atencionId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"atencionPeluqueria"})
    java.util.List<FotoPeluqueria> findAllByAtencionPeluqueriaIdOrderByCreatedAtAscIdAsc(Long atencionId);
}
