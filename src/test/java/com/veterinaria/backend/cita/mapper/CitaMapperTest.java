package com.veterinaria.backend.cita.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.veterinaria.backend.cita.dto.CitaDetalleResponse;
import com.veterinaria.backend.cita.dto.CitaResumenResponse;
import com.veterinaria.backend.cita.dto.CitaUsuarioResponse;
import com.veterinaria.backend.cita.dto.CrearCitaRequest;
import com.veterinaria.backend.cita.dto.DisponibilidadCitaResponse;
import com.veterinaria.backend.cita.dto.ServicioCitaRequest;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TamanoMascota;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.usuario.entity.Usuario;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CitaMapperTest {

    private final CitaMapper mapper = new CitaMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldMapResumenWithNestedNamesAndServiceSnapshots() {
        Cita cita = cita();
        List<CitaServicio> citaServicios = List.of(consultaMedica(cita), banoMedicado(cita));

        CitaResumenResponse response = mapper.toResumen(cita, citaServicios);

        assertThat(response.id()).isEqualTo(91L);
        assertThat(response.tipoCita()).isEqualTo(TipoCita.MEDICA);
        assertThat(response.estado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(response.fechaHoraInicio()).isEqualTo(cita.getFechaHoraInicio());
        assertThat(response.fechaHoraFin()).isEqualTo(cita.getFechaHoraFin());
        assertThat(response.mascota().id()).isEqualTo(8L);
        assertThat(response.mascota().nombre()).isEqualTo("Luna");
        assertThat(response.cliente().id()).isEqualTo(7L);
        assertThat(response.cliente().nombreCompleto()).isEqualTo("Maria Elena Torres");
        assertThat(response.trabajador().id()).isEqualTo(11L);
        assertThat(response.trabajador().nombreCompleto()).isEqualTo("Julio Cesar Paredes");
        assertThat(response.servicios())
                .extracting(servicio -> servicio.id(), servicio -> servicio.nombre(), servicio -> servicio.precioAplicado(),
                        servicio -> servicio.duracionAplicadaMinutos(), servicio -> servicio.precioServicioTamanoId())
                .containsExactly(
                        tuple(21L, "Consulta general", new BigDecimal("80.00"), 45, null),
                        tuple(22L, "Baño medicado", new BigDecimal("65.50"), 30, 301L));
    }

    @Test
    void shouldMapDetalleWithAuditAndOptionalFields() {
        Cita cita = cita();
        List<CitaServicio> citaServicios = List.of(consultaMedica(cita), banoMedicado(cita));

        CitaDetalleResponse response = mapper.toDetalle(cita, citaServicios);

        assertThat(response.id()).isEqualTo(91L);
        assertThat(response.motivoConsulta()).isEqualTo("Control anual");
        assertThat(response.observaciones()).isEqualTo("Llegar 10 minutos antes");
        assertThat(response.motivoCancelacion()).isNull();
        assertThat(response.motivoNoAtencion()).isNull();
        assertThat(response.createdAt()).isEqualTo(cita.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(cita.getUpdatedAt());
        CitaUsuarioResponse registradoPor = response.registradoPor();
        assertThat(registradoPor.id()).isEqualTo(13L);
        assertThat(registradoPor.nombreCompleto()).isEqualTo("Paola Ruiz");
        assertThat(response.servicios())
                .extracting(servicio -> servicio.id(), servicio -> servicio.nombre(), servicio -> servicio.tipoServicio())
                .containsExactly(
                        tuple(21L, "Consulta general", TipoServicio.MEDICO),
                        tuple(22L, "Baño medicado", TipoServicio.PELUQUERIA));
    }

    @Test
    void shouldRejectNullServiceEntriesInCrearCitaRequest() {
        CrearCitaRequest request = new CrearCitaRequest(
                8L,
                11L,
                now().plusDays(2).withHour(10).withMinute(0),
                Collections.singletonList(null),
                "Control anual",
                "Observaciones");

        Set<ConstraintViolation<CrearCitaRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).contains("servicios");
                    assertThat(violation.getMessageTemplate()).isEqualTo("{jakarta.validation.constraints.NotNull.message}");
                });
    }

    @Test
    void shouldBuildFullNamesWithoutBlankSegments() {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("  Maria ");
        cliente.setSegundoNombre(" ");
        cliente.setPrimerApellido(" Torres ");
        cliente.setSegundoApellido(null);

        Usuario usuario = new Usuario();
        usuario.setPrimerNombre(" Julio ");
        usuario.setSegundoNombre(" Cesar ");
        usuario.setPrimerApellido(" ");
        usuario.setSegundoApellido(" Paredes ");

        assertThat(mapper.nombreCompleto(cliente)).isEqualTo("Maria Torres");
        assertThat(mapper.nombreCompleto(usuario)).isEqualTo("Julio Cesar Paredes");
    }

    @Test
    void shouldMapDisponibilidadWithBaseAndOverlapFlags() {
        LocalDateTime inicio = now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime fin = inicio.plusMinutes(45);

        DisponibilidadCitaResponse response = mapper.toDisponibilidad(
                11L,
                TipoCita.MEDICA,
                inicio,
                fin,
                true,
                false,
                true,
                " Disponible ");

        assertThat(response.trabajadorId()).isEqualTo(11L);
        assertThat(response.tipoCita()).isEqualTo(TipoCita.MEDICA);
        assertThat(response.disponibilidadBase()).isTrue();
        assertThat(response.tieneCitaSolapada()).isFalse();
        assertThat(response.disponible()).isTrue();
        assertThat(response.motivo()).isEqualTo("Disponible");
    }

    private Cita cita() {
        LocalDateTime baseNow = now();

        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setPrimerNombre(" Maria ");
        cliente.setSegundoNombre("Elena");
        cliente.setPrimerApellido(" Torres ");
        cliente.setSegundoApellido(" ");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("12345678");
        cliente.setActivo(Boolean.TRUE);

        Mascota mascota = new Mascota();
        mascota.setId(8L);
        mascota.setCliente(cliente);
        mascota.setNombre("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setActivo(Boolean.TRUE);

        Usuario trabajador = new Usuario();
        trabajador.setId(11L);
        trabajador.setPrimerNombre(" Julio ");
        trabajador.setSegundoNombre("Cesar");
        trabajador.setPrimerApellido(" ");
        trabajador.setSegundoApellido("Paredes");
        trabajador.setCorreo("julio@test.dev");
        trabajador.setPasswordHash("hash");
        trabajador.setActivo(Boolean.TRUE);
        trabajador.setIntentosFallidos(0);

        Usuario registradoPor = new Usuario();
        registradoPor.setId(13L);
        registradoPor.setPrimerNombre("Paola");
        registradoPor.setPrimerApellido("Ruiz");
        registradoPor.setCorreo("paola@test.dev");
        registradoPor.setPasswordHash("hash");
        registradoPor.setActivo(Boolean.TRUE);
        registradoPor.setIntentosFallidos(0);

        Cita cita = new Cita();
        cita.setId(91L);
        cita.setMascota(mascota);
        cita.setTrabajadorAsignado(trabajador);
        cita.setRegistradoPor(registradoPor);
        cita.setTipoCita(TipoCita.MEDICA);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(baseNow.plusDays(2).withHour(10).withMinute(0));
        cita.setFechaHoraFin(baseNow.plusDays(2).withHour(11).withMinute(15));
        cita.setMotivoConsulta("Control anual");
        cita.setObservaciones("Llegar 10 minutos antes");
        cita.setCreatedAt(baseNow.minusDays(1).withHour(9).withMinute(0));
        cita.setUpdatedAt(baseNow.minusDays(1).withHour(9).withMinute(30));
        return cita;
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withSecond(0).withNano(0);
    }

    private CitaServicio consultaMedica(Cita cita) {
        Servicio servicio = new Servicio();
        servicio.setId(21L);
        servicio.setNombre("Consulta general");
        servicio.setTipoServicio(TipoServicio.MEDICO);
        servicio.setDuracionMinutos(45);
        servicio.setActivo(Boolean.TRUE);

        CitaServicio citaServicio = new CitaServicio();
        citaServicio.setId(201L);
        citaServicio.setCita(cita);
        citaServicio.setServicio(servicio);
        citaServicio.setPrecioAplicado(new BigDecimal("80.00"));
        citaServicio.setDuracionAplicadaMinutos(45);
        return citaServicio;
    }

    private CitaServicio banoMedicado(Cita cita) {
        Servicio servicio = new Servicio();
        servicio.setId(22L);
        servicio.setNombre("Baño medicado");
        servicio.setTipoServicio(TipoServicio.PELUQUERIA);
        servicio.setDuracionMinutos(30);
        servicio.setActivo(Boolean.TRUE);

        PrecioServicioTamano precioServicioTamano = new PrecioServicioTamano();
        precioServicioTamano.setId(301L);
        precioServicioTamano.setServicio(servicio);
        precioServicioTamano.setTamanoMascota(TamanoMascota.MEDIANO);
        precioServicioTamano.setPrecio(new BigDecimal("65.50"));
        precioServicioTamano.setDuracionMinutos(30);
        precioServicioTamano.setActivo(Boolean.TRUE);

        CitaServicio citaServicio = new CitaServicio();
        citaServicio.setId(202L);
        citaServicio.setCita(cita);
        citaServicio.setServicio(servicio);
        citaServicio.setPrecioServicioTamano(precioServicioTamano);
        citaServicio.setPrecioAplicado(new BigDecimal("65.50"));
        citaServicio.setDuracionAplicadaMinutos(30);
        return citaServicio;
    }

    private static org.assertj.core.groups.Tuple tuple(
            Object value1,
            Object value2,
            Object value3,
            Object value4,
            Object value5) {
        return org.assertj.core.groups.Tuple.tuple(value1, value2, value3, value4, value5);
    }

    private static org.assertj.core.groups.Tuple tuple(
            Object value1,
            Object value2,
            Object value3) {
        return org.assertj.core.groups.Tuple.tuple(value1, value2, value3);
    }
}
