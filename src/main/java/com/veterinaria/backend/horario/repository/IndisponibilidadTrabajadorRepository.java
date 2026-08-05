package com.veterinaria.backend.horario.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.horario.entity.IndisponibilidadTrabajador;

public interface IndisponibilidadTrabajadorRepository extends JpaRepository<IndisponibilidadTrabajador, Long> {

    @Query("""
            select count(indisponibilidad) > 0 from IndisponibilidadTrabajador indisponibilidad
            where indisponibilidad.usuario.id = :usuarioId
              and indisponibilidad.fechaInicio < :fechaFin
              and indisponibilidad.fechaFin > :fechaInicio
              and (:excluirId is null or indisponibilidad.id <> :excluirId)
            """)
    boolean existsSolapamiento(@Param("usuarioId") Long usuarioId, @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin, @Param("excluirId") Long excluirId);

    @EntityGraph(attributePaths = "usuario")
    List<IndisponibilidadTrabajador> findByUsuarioIdOrderByFechaInicioAsc(Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    java.util.Optional<IndisponibilidadTrabajador> findByIdAndUsuarioId(Long id, Long usuarioId);
}
