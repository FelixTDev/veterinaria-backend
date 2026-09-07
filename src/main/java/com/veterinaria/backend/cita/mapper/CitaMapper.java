package com.veterinaria.backend.cita.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import com.veterinaria.backend.cita.dto.CitaClienteResponse;
import com.veterinaria.backend.cita.dto.CitaDetalleResponse;
import com.veterinaria.backend.cita.dto.CitaMascotaResponse;
import com.veterinaria.backend.cita.dto.CitaResumenResponse;
import com.veterinaria.backend.cita.dto.CitaServicioResponse;
import com.veterinaria.backend.cita.dto.CitaTrabajadorResponse;
import com.veterinaria.backend.cita.dto.CitaUsuarioResponse;
import com.veterinaria.backend.cita.dto.DisponibilidadCitaResponse;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.usuario.entity.Usuario;

import org.springframework.stereotype.Component;

@Component
public class CitaMapper {

    public CitaResumenResponse toResumen(Cita cita, List<CitaServicio> servicios) {
        return new CitaResumenResponse(
                cita.getId(),
                cita.getTipoCita(),
                cita.getEstado(),
                cita.getFechaHoraInicio(),
                cita.getFechaHoraFin(),
                toMascota(cita.getMascota()),
                toCliente(cita.getMascota().getCliente()),
                toTrabajador(cita.getTrabajadorAsignado()),
                servicios.stream().map(this::toServicio).toList());
    }

    public CitaDetalleResponse toDetalle(Cita cita, List<CitaServicio> servicios) {
        return new CitaDetalleResponse(
                cita.getId(),
                cita.getTipoCita(),
                cita.getEstado(),
                cita.getFechaHoraInicio(),
                cita.getFechaHoraFin(),
                trimToNull(cita.getMotivoConsulta()),
                trimToNull(cita.getMotivoNoAtencion()),
                trimToNull(cita.getMotivoCancelacion()),
                trimToNull(cita.getObservaciones()),
                toMascota(cita.getMascota()),
                toCliente(cita.getMascota().getCliente()),
                toTrabajador(cita.getTrabajadorAsignado()),
                toUsuario(cita.getRegistradoPor()),
                servicios.stream().map(this::toServicio).toList(),
                cita.getCreatedAt(),
                cita.getUpdatedAt());
    }

    public DisponibilidadCitaResponse toDisponibilidad(
            Long trabajadorId,
            com.veterinaria.backend.cita.enums.TipoCita tipoCita,
            LocalDateTime inicio,
            LocalDateTime fin,
            boolean disponibilidadBase,
            boolean tieneCitaSolapada,
            boolean disponible,
            String motivo) {
        return new DisponibilidadCitaResponse(
                trabajadorId,
                tipoCita,
                inicio,
                fin,
                disponibilidadBase,
                tieneCitaSolapada,
                disponible,
                trimToNull(motivo));
    }

    public String nombreCompleto(Cliente cliente) {
        return Stream.of(
                        cliente.getPrimerNombre(),
                        cliente.getSegundoNombre(),
                        cliente.getPrimerApellido(),
                        cliente.getSegundoApellido())
                .map(this::trimToNull)
                .filter(value -> value != null)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    public String nombreCompleto(Usuario usuario) {
        return Stream.of(
                        usuario.getPrimerNombre(),
                        usuario.getSegundoNombre(),
                        usuario.getPrimerApellido(),
                        usuario.getSegundoApellido())
                .map(this::trimToNull)
                .filter(value -> value != null)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private CitaMascotaResponse toMascota(Mascota mascota) {
        return new CitaMascotaResponse(
                mascota.getId(),
                mascota.getNombre(),
                mascota.getEspecie(),
                trimToNull(mascota.getRaza()));
    }

    private CitaClienteResponse toCliente(Cliente cliente) {
        return new CitaClienteResponse(
                cliente.getId(),
                nombreCompleto(cliente),
                trimToNull(cliente.getTelefono()),
                trimToNull(cliente.getCorreo()));
    }

    private CitaTrabajadorResponse toTrabajador(Usuario usuario) {
        return new CitaTrabajadorResponse(
                usuario.getId(),
                nombreCompleto(usuario),
                usuario.getCorreo());
    }

    private CitaUsuarioResponse toUsuario(Usuario usuario) {
        return new CitaUsuarioResponse(
                usuario.getId(),
                nombreCompleto(usuario),
                usuario.getCorreo());
    }

    private CitaServicioResponse toServicio(CitaServicio citaServicio) {
        return new CitaServicioResponse(
                citaServicio.getServicio().getId(),
                citaServicio.getServicio().getNombre(),
                citaServicio.getServicio().getTipoServicio(),
                citaServicio.getPrecioServicioTamano() != null ? citaServicio.getPrecioServicioTamano().getId() : null,
                citaServicio.getPrecioAplicado(),
                citaServicio.getDuracionAplicadaMinutos());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
