package com.veterinaria.backend.usuario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.entity.UsuarioRolId;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {

    List<UsuarioRol> findByUsuario_Id(Long usuarioId);
}
