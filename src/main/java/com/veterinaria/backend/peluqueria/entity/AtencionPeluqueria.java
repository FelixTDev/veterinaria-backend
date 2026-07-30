package com.veterinaria.backend.peluqueria.entity;

import java.time.LocalDateTime;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.shared.entity.BaseAuditableEntity;
import com.veterinaria.backend.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "atenciones_peluqueria")
public class AtencionPeluqueria extends BaseAuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peluquero_id", nullable = false)
    private Usuario peluquero;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_atencion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaAtencion;
}
