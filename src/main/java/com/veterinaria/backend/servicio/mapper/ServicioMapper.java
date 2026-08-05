package com.veterinaria.backend.servicio.mapper;

import java.util.List;
import com.veterinaria.backend.servicio.dto.PrecioServicioResponse;
import com.veterinaria.backend.servicio.dto.ServicioDetalleResponse;
import com.veterinaria.backend.servicio.dto.ServicioResumenResponse;
import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {
    public ServicioResumenResponse toResumen(Servicio servicio, boolean preciosConfigurados) {
        return new ServicioResumenResponse(servicio.getId(), servicio.getNombre(), servicio.getTipoServicio(),
                servicio.getDuracionMinutos(), servicio.getActivo(), preciosConfigurados);
    }
    public ServicioDetalleResponse toDetalle(Servicio servicio, List<PrecioServicioTamano> precios) {
        return new ServicioDetalleResponse(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getTipoServicio(), servicio.getPrecioBase(), servicio.getDuracionMinutos(), servicio.getActivo(),
                servicio.getCreatedAt(), servicio.getUpdatedAt(), precios.stream().map(this::toPrecio).toList());
    }
    public PrecioServicioResponse toPrecio(PrecioServicioTamano precio) {
        return new PrecioServicioResponse(precio.getId(), precio.getTamanoMascota(), precio.getPrecio(),
                precio.getDuracionMinutos(), precio.getActivo());
    }
}
