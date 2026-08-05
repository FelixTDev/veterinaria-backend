package com.veterinaria.backend.cliente.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
            TipoDocumento tipoDocumento, String numeroDocumento, Long id);

    @Query("""
            select cliente from Cliente cliente
            where (:activo is null or cliente.activo = :activo)
              and (:tipoDocumento is null or cliente.tipoDocumento = :tipoDocumento)
              and (:search is null or :search = ''
                   or lower(cliente.primerNombre) like lower(concat('%', :search, '%'))
                   or lower(coalesce(cliente.segundoNombre, '')) like lower(concat('%', :search, '%'))
                   or lower(cliente.primerApellido) like lower(concat('%', :search, '%'))
                   or lower(coalesce(cliente.segundoApellido, '')) like lower(concat('%', :search, '%'))
                   or lower(cliente.numeroDocumento) like lower(concat('%', :search, '%'))
                   or lower(coalesce(cliente.correo, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(cliente.telefono, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Cliente> findAllForGestion(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("tipoDocumento") TipoDocumento tipoDocumento,
            Pageable pageable);

    @Query("""
            select mascota.cliente.id, count(mascota.id)
            from Mascota mascota
            where mascota.cliente.id in :clienteIds
            group by mascota.cliente.id
            """)
    List<Object[]> countMascotasByClienteIds(@Param("clienteIds") List<Long> clienteIds);
}
