package com.veterinaria.backend.horario.entity;

import java.time.LocalTime;

import com.veterinaria.backend.shared.entity.BaseAuditableEntity;
import com.veterinaria.backend.usuario.entity.Usuario;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "horarios_trabajador")
public class HorarioTrabajador extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "descanso_inicio")
    private LocalTime descansoInicio;

    @Column(name = "descanso_fin")
    private LocalTime descansoFin;

    @Column(name = "disponible", nullable = false)
    private Boolean disponible;
}
