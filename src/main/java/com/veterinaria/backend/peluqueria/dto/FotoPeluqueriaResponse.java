package com.veterinaria.backend.peluqueria.dto;

import java.time.LocalDateTime;

import com.veterinaria.backend.peluqueria.enums.TipoFoto;

public record FotoPeluqueriaResponse(
        Long id,
        TipoFoto tipoFoto,
        String urlArchivo,
        String nombreArchivo,
        LocalDateTime createdAt) {
}
