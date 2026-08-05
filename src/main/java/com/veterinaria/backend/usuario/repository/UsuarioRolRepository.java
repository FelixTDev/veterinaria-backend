package com.veterinaria.backend.usuario.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.entity.UsuarioRolId;
import com.veterinaria.backend.usuario.enums.NombreRol;

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

    @Query("""
            select usuarioRol
            from UsuarioRol usuarioRol
            join fetch usuarioRol.usuario usuario
            join fetch usuarioRol.rol rol
            where usuario.id in :usuarioIds
              and rol.activo = true
            order by usuario.id asc, rol.nombre asc
            """)
    List<UsuarioRol> findActivosByUsuarioIds(@Param("usuarioIds") List<Long> usuarioIds);

    boolean existsByUsuario_IdAndRol_Nombre(Long usuarioId, NombreRol nombreRol);

    boolean existsByUsuario_IdAndRol_NombreIn(Long usuarioId, Collection<NombreRol> nombres);

    void deleteByUsuario_Id(Long usuarioId);

    @Query("""
            select count(distinct usuario.id)
            from UsuarioRol usuarioRol
            join usuarioRol.usuario usuario
            join usuarioRol.rol rol
            where usuario.activo = true
              and rol.activo = true
              and rol.nombre = com.veterinaria.backend.usuario.enums.NombreRol.ADMINISTRADOR
            """)
    long countActiveAdministradores();
}
