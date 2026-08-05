package com.veterinaria.backend.mascota.mapper;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.mascota.dto.MascotaDetalleResponse;
import com.veterinaria.backend.mascota.dto.MascotaResumenResponse;
import com.veterinaria.backend.mascota.entity.Mascota;
import org.springframework.stereotype.Component;

@Component
public class MascotaMapper {

    public MascotaResumenResponse toResumen(Mascota mascota) {
        return new MascotaResumenResponse(
                mascota.getId(), mascota.getCliente().getId(), propietario(mascota.getCliente()), mascota.getNombre(),
                mascota.getEspecie(), mascota.getRaza(), mascota.getSexo(), mascota.getActivo());
    }

    public MascotaDetalleResponse toDetalle(Mascota mascota) {
        return new MascotaDetalleResponse(
                mascota.getId(), mascota.getCliente().getId(), propietario(mascota.getCliente()), mascota.getNombre(),
                mascota.getEspecie(), mascota.getRaza(), mascota.getColor(), mascota.getSexo(), mascota.getPesoKg(),
                mascota.getFechaNacimiento(), mascota.getEdadAproximadaAnios(), mascota.getActivo(),
                mascota.getCreatedAt(), mascota.getUpdatedAt());
    }

    private String propietario(Cliente cliente) {
        return String.join(" ", java.util.stream.Stream.of(
                        cliente.getPrimerNombre(), cliente.getSegundoNombre(),
                        cliente.getPrimerApellido(), cliente.getSegundoApellido())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList());
    }
}
