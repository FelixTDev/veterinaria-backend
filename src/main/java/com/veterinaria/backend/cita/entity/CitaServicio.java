package com.veterinaria.backend.cita.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
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
@Table(name = "cita_servicios")
public class CitaServicio extends BaseCreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "precio_servicio_tamano_id")
    private PrecioServicioTamano precioServicioTamano;

    @Column(name = "precio_aplicado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioAplicado;

    @Column(name = "duracion_aplicada_minutos", nullable = false)
    private Integer duracionAplicadaMinutos;
}
