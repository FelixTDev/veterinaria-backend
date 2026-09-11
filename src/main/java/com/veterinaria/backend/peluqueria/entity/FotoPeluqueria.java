package com.veterinaria.backend.peluqueria.entity;

import com.veterinaria.backend.peluqueria.enums.TipoFoto;
import com.veterinaria.backend.shared.entity.BaseCreatableEntity;

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
@Table(name = "fotos_peluqueria")
public class FotoPeluqueria extends BaseCreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_peluqueria_id", nullable = false)
    private AtencionPeluqueria atencionPeluqueria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_foto", nullable = false, length = 15)
    private TipoFoto tipoFoto;

    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;
}
