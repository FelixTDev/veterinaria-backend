package com.veterinaria.backend.servicio.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;

public interface PrecioServicioTamanoRepository extends JpaRepository<PrecioServicioTamano, Long> {

    @Override
    @EntityGraph(attributePaths = "servicio")
    Optional<PrecioServicioTamano> findById(Long id);

    @EntityGraph(attributePaths = "servicio")
    List<PrecioServicioTamano> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "servicio")
    default List<PrecioServicioTamano> findByIdInWithServicio(Collection<Long> ids) {
        return findByIdIn(ids);
    }

    List<PrecioServicioTamano> findByServicioNombre(String nombreServicio);

    List<PrecioServicioTamano> findByServicioIdOrderByTamanoMascotaAsc(Long servicioId);

    List<PrecioServicioTamano> findByServicioId(Long servicioId);

    @org.springframework.data.jpa.repository.Query("""
            select precio.servicio.id, count(precio.id)
            from PrecioServicioTamano precio
            where precio.servicio.id in :servicioIds and precio.activo = true
            group by precio.servicio.id
            """)
    List<Object[]> countActivosByServicioIds(@org.springframework.data.repository.query.Param("servicioIds") Collection<Long> servicioIds);
}
