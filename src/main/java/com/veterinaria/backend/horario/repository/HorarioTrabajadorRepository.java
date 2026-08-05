package com.veterinaria.backend.horario.repository;

import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.horario.entity.HorarioTrabajador;

public interface HorarioTrabajadorRepository extends JpaRepository<HorarioTrabajador, Long> {

    @Query("""
            select count(horario) > 0 from HorarioTrabajador horario
            where horario.usuario.id = :usuarioId and horario.diaSemana = :diaSemana
              and horario.disponible = true
              and horario.horaInicio < :horaFin and horario.horaFin > :horaInicio
              and (:excluirId is null or horario.id <> :excluirId)
            """)
    boolean existsSolapamiento(@Param("usuarioId") Long usuarioId, @Param("diaSemana") Integer diaSemana,
            @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
            @Param("excluirId") Long excluirId);

    @Query("""
            select count(horario) > 0 from HorarioTrabajador horario
            where horario.usuario.id = :usuarioId and horario.diaSemana = :diaSemana
              and horario.disponible = true
              and horario.horaInicio <= :horaInicio and horario.horaFin >= :horaFin
              and (horario.descansoInicio is null
                   or :horaFin <= horario.descansoInicio
                   or :horaInicio >= horario.descansoFin)
            """)
    boolean existsHorarioQueCubre(@Param("usuarioId") Long usuarioId, @Param("diaSemana") Integer diaSemana,
            @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin);

    @EntityGraph(attributePaths = "usuario")
    List<HorarioTrabajador> findByUsuarioIdOrderByDiaSemanaAscHoraInicioAsc(Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    java.util.Optional<HorarioTrabajador> findByIdAndUsuarioId(Long id, Long usuarioId);
}
