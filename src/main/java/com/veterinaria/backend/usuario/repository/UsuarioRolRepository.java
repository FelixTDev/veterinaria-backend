package com.veterinaria.backend.usuario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.entity.UsuarioRolId;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {

    List<UsuarioRol> findByUsuario_Id(Long usuarioId);

    @Query("""
            select usuarioRol
            from UsuarioRol usuarioRol
            join fetch usuarioRol.rol rol
            where usuarioRol.usuario.id = :usuarioId
              and rol.activo = true
            """)
    List<UsuarioRol> findActivosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
