package com.veterinaria.backend.vacuna.entity;

import java.time.LocalDate;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.shared.entity.BaseCreatableEntity;

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
@Table(name = "vacunas_aplicadas")
public class VacunaAplicada extends BaseCreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_medica_id", nullable = false)
    private AtencionMedica atencionMedica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vacuna_id", nullable = false)
    private Vacuna vacuna;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "proxima_fecha")
    private LocalDate proximaFecha;

    @Column(name = "lote", length = 80)
    private String lote;

    @Column(name = "observaciones", length = 300)
    private String observaciones;
}
