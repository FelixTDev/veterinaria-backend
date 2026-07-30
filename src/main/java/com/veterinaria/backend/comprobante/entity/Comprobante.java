package com.veterinaria.backend.comprobante.entity;

import java.time.LocalDateTime;

import com.veterinaria.backend.comprobante.enums.EstadoComprobante;
import com.veterinaria.backend.comprobante.enums.TipoComprobante;
import com.veterinaria.backend.pago.entity.Pago;
import com.veterinaria.backend.shared.entity.BaseAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comprobantes")
public class Comprobante extends BaseAuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false, unique = true)
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 10)
    private TipoComprobante tipoComprobante;

    @Column(name = "serie", length = 10)
    private String serie;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "direccion_fiscal", length = 300)
    private String direccionFiscal;

    @Column(name = "fecha_emision", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoComprobante estado;
}
