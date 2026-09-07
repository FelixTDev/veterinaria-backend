package com.veterinaria.backend.mascota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;
import com.veterinaria.backend.mascota.entity.Mascota;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByClienteId(Long clienteId);

    @Override
    @EntityGraph(attributePaths = "cliente")
    Optional<Mascota> findById(Long id);

    @EntityGraph(attributePaths = "cliente")
    @Query("""
            select mascota from Mascota mascota
            where (:clienteId is null or mascota.cliente.id = :clienteId)
              and (:activo is null or mascota.activo = :activo)
              and (:especie is null or mascota.especie = :especie)
              and (:sexo is null or mascota.sexo = :sexo)
              and (:search is null or :search = ''
                   or lower(mascota.nombre) like lower(concat('%', :search, '%'))
                   or lower(coalesce(mascota.raza, '')) like lower(concat('%', :search, '%'))
                   or lower(mascota.especie) like lower(concat('%', :search, '%'))
                   or lower(coalesce(mascota.cliente.primerNombre, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(mascota.cliente.primerApellido, '')) like lower(concat('%', :search, '%'))
                   or lower(mascota.cliente.numeroDocumento) like lower(concat('%', :search, '%')))
            """)
    Page<Mascota> findAllForGestion(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("clienteId") Long clienteId,
            @Param("especie") EspecieMascota especie,
            @Param("sexo") SexoMascota sexo,
            Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Mascota> findByClienteIdAndActivo(Long clienteId, Boolean activo, Pageable pageable);
}
