package com.veterinaria.backend.mascota.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.enums.SexoMascota;
import com.veterinaria.backend.shared.entity.BaseAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "mascotas")
public class Mascota extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "especie", nullable = false, length = 30)
    private EspecieMascota especie;

    @Column(name = "raza", length = 80)
    private String raza;

    @Column(name = "color", length = 80)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 15)
    private SexoMascota sexo;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "edad_aproximada_anios")
    private Integer edadAproximadaAnios;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
