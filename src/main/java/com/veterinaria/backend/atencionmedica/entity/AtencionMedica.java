package com.veterinaria.backend.atencionmedica.entity;

import java.math.BigDecimal;
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
@Table(name = "atenciones_medicas")
public class AtencionMedica extends BaseAuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Usuario veterinario;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "temperatura_c", precision = 4, scale = 1)
    private BigDecimal temperaturaC;

    @Column(name = "sintomas", columnDefinition = "TEXT")
    private String sintomas;

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "motivo_sin_diagnostico", columnDefinition = "TEXT")
    private String motivoSinDiagnostico;

    @Column(name = "tratamiento", columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "motivo_sin_tratamiento", columnDefinition = "TEXT")
    private String motivoSinTratamiento;

    @Column(name = "receta", columnDefinition = "TEXT")
    private String receta;

    @Column(name = "motivo_sin_receta", columnDefinition = "TEXT")
    private String motivoSinReceta;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_atencion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaAtencion;
}
