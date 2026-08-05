package com.veterinaria.backend.usuario.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.enums.NombreRol;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(NombreRol nombre);

    List<Rol> findAllByActivoTrueOrderByNombreAsc();

    List<Rol> findAllByNombreIn(Collection<NombreRol> nombres);
}
