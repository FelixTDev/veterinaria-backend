package com.veterinaria.backend.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.enums.NombreRol;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(NombreRol nombre);
}
