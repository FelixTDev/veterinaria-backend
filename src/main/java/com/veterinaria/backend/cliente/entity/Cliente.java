package com.veterinaria.backend.cliente.entity;

import java.time.LocalDate;

import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.shared.entity.BaseAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente extends BaseAuditableEntity {

    @Column(name = "primer_nombre", nullable = false, length = 60)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 60)
    private String segundoNombre;

    @Column(name = "primer_apellido", nullable = false, length = 60)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 60)
    private String segundoApellido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
