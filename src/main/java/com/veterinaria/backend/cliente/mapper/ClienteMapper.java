package com.veterinaria.backend.cliente.mapper;

import java.util.List;

import com.veterinaria.backend.cliente.dto.ClienteDetalleResponse;
import com.veterinaria.backend.cliente.dto.ClienteResumenResponse;
import com.veterinaria.backend.cliente.dto.MascotaClienteResumenResponse;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.mascota.entity.Mascota;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteResumenResponse toResumen(Cliente cliente, long cantidadMascotas) {
        return new ClienteResumenResponse(
                cliente.getId(), nombreCompleto(cliente), cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(), cliente.getCorreo(), cliente.getTelefono(),
                cliente.getActivo(), cantidadMascotas);
    }

    public ClienteDetalleResponse toDetalle(Cliente cliente, List<Mascota> mascotas) {
        return new ClienteDetalleResponse(
                cliente.getId(), trimToNull(cliente.getPrimerNombre()), trimToNull(cliente.getSegundoNombre()),
                trimToNull(cliente.getPrimerApellido()), trimToNull(cliente.getSegundoApellido()),
                nombreCompleto(cliente), cliente.getTipoDocumento(), cliente.getNumeroDocumento(),
                cliente.getFechaNacimiento(), cliente.getTelefono(), cliente.getCorreo(), cliente.getActivo(),
                cliente.getCreatedAt(), cliente.getUpdatedAt(), mascotas.stream().map(this::toMascotaResumen).toList());
    }

    public String nombreCompleto(Cliente cliente) {
        StringBuilder builder = new StringBuilder();
        append(builder, cliente.getPrimerNombre());
        append(builder, cliente.getSegundoNombre());
        append(builder, cliente.getPrimerApellido());
        append(builder, cliente.getSegundoApellido());
        return builder.toString();
    }

    private MascotaClienteResumenResponse toMascotaResumen(Mascota mascota) {
        return new MascotaClienteResumenResponse(
                mascota.getId(), mascota.getNombre(), mascota.getEspecie().name(), mascota.getActivo());
    }

    private void append(StringBuilder builder, String value) {
        String normalized = trimToNull(value);
        if (normalized != null) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(normalized);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
