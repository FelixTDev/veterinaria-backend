package com.veterinaria.backend.usuario.repository;

import java.util.Optional;

import com.veterinaria.backend.usuario.enums.NombreRol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.usuario.entity.Usuario;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usuario from Usuario usuario where lower(usuario.correo) = lower(:correo)")
    Optional<Usuario> findByCorreoIgnoreCaseForUpdate(@Param("correo") String correo);


    boolean existsByCorreo(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByIdAndActivoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select usuario
            from Usuario usuario
            where usuario.id = :id
            """)
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select usuario
            from Usuario usuario
            where (:activo is null or usuario.activo = :activo)
              and (:search is null or :search = ''
                   or lower(usuario.primerNombre) like lower(concat('%', :search, '%'))
                   or lower(coalesce(usuario.segundoNombre, '')) like lower(concat('%', :search, '%'))
                   or lower(usuario.primerApellido) like lower(concat('%', :search, '%'))
                   or lower(coalesce(usuario.segundoApellido, '')) like lower(concat('%', :search, '%'))
                   or lower(usuario.correo) like lower(concat('%', :search, '%')))
              and (:rol is null or exists (
                   select 1
                   from UsuarioRol usuarioRol
                   join usuarioRol.rol rol
                   where usuarioRol.usuario = usuario
                     and rol.nombre = :rol
                     and rol.activo = true))
            """)
    Page<Usuario> findAllForGestion(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("rol") NombreRol rol,
            Pageable pageable);
}
