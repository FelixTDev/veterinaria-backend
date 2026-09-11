package com.veterinaria.backend.reporte.projection;

public interface ServicioRankingProjection {
    Long getServicioId();
    String getNombre();
    String getTipoServicio();
    long getCantidad();
}
