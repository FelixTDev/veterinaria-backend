package com.veterinaria.backend.servicio.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;

public interface PrecioServicioTamanoRepository extends JpaRepository<PrecioServicioTamano, Long> {

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
