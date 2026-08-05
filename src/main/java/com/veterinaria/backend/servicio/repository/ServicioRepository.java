package com.veterinaria.backend.servicio.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.veterinaria.backend.servicio.enums.TipoServicio;

import com.veterinaria.backend.servicio.entity.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    Optional<Servicio> findByNombre(String nombre);

    Optional<Servicio> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
            select servicio from Servicio servicio
            where (:activo is null or servicio.activo = :activo)
              and (:tipoServicio is null or servicio.tipoServicio = :tipoServicio)
              and (:search is null or :search = ''
                   or lower(servicio.nombre) like lower(concat('%', :search, '%'))
                   or lower(coalesce(servicio.descripcion, '')) like lower(concat('%', :search, '%'))
                   or lower(servicio.tipoServicio) like lower(concat('%', :search, '%')))
            """)
    Page<Servicio> findAllForGestion(@Param("search") String search, @Param("activo") Boolean activo,
            @Param("tipoServicio") TipoServicio tipoServicio, Pageable pageable);
}
