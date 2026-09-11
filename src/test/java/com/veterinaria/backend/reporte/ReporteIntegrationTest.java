package com.veterinaria.backend.reporte;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.atencionmedica.repository.AtencionMedicaRepository;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.repository.CitaServicioRepository;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.comprobante.entity.Comprobante;
import com.veterinaria.backend.comprobante.enums.EstadoComprobante;
import com.veterinaria.backend.comprobante.enums.TipoComprobante;
import com.veterinaria.backend.comprobante.repository.ComprobanteRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.pago.entity.DetallePago;
import com.veterinaria.backend.pago.entity.Pago;
import com.veterinaria.backend.pago.enums.EstadoPago;
import com.veterinaria.backend.pago.enums.MedioPago;
import com.veterinaria.backend.pago.repository.DetallePagoRepository;
import com.veterinaria.backend.pago.repository.PagoRepository;
import com.veterinaria.backend.reporte.dto.GranularidadReporte;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.vacuna.entity.Vacuna;
import com.veterinaria.backend.vacuna.entity.VacunaAplicada;
import com.veterinaria.backend.vacuna.repository.VacunaRepository;
import com.veterinaria.backend.vacuna.repository.VacunaAplicadaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReporteIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired MockMvc mockMvc;
    @Autowired ClienteRepository clientes;
    @Autowired MascotaRepository mascotas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ServicioRepository servicios;
    @Autowired CitaRepository citas;
    @Autowired CitaServicioRepository citaServicios;
    @Autowired PagoRepository pagos;
    @Autowired DetallePagoRepository detalles;
    @Autowired ComprobanteRepository comprobantes;
    @Autowired AtencionMedicaRepository atencionesMedicas;
    @Autowired VacunaRepository vacunas;
    @Autowired VacunaAplicadaRepository vacunasAplicadas;

    private LocalDateTime desde;
    private LocalDateTime hasta;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        vacunasAplicadas.deleteAll();
        atencionesMedicas.deleteAll();
        comprobantes.deleteAll();
        detalles.deleteAll();
        pagos.deleteAll();
        citaServicios.deleteAll();
        citas.deleteAll();
        vacunas.deleteAll();
        servicios.deleteAll();
        mascotas.deleteAll();
        clientes.deleteAll();
        usuarios.deleteAll();
        desde = LocalDateTime.now().minusDays(2).withNano(0);
        hasta = LocalDateTime.now().plusDays(2).withNano(0);
        fixture = crearFixture();
    }

    @Test
    void aggregatesPaymentsPartialsMixedAndExcludesPendingAndCancelled() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/finanzas/resumen")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresoTotal").value(150.0))
                .andExpect(jsonPath("$.cantidadPagos").value(3))
                .andExpect(jsonPath("$.ingresoPorMedio[?(@.medioPago == 'EFECTIVO')].monto").value(90.0))
                .andExpect(jsonPath("$.ingresoPorMedio[?(@.medioPago == 'YAPE')].monto").value(60.0));

        mockMvc.perform(get("/api/v1/reportes/pagos/saldos-pendientes")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalServicios").value(120.0))
                .andExpect(jsonPath("$.content[0].totalPagado").value(50.0))
                .andExpect(jsonPath("$.content[0].saldoPendiente").value(70.0));
    }

    @Test
    void groupsAppointmentsServicesTrendVaccinesAndReceipts() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/citas/resumen")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VETERINARIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.porEstado[?(@.estado == 'ATENDIDA')].cantidad").value(2))
                .andExpect(jsonPath("$.porEstado[?(@.estado == 'CANCELADA')].cantidad").value(1))
                .andExpect(jsonPath("$.porTipo[?(@.tipo == 'MEDICA')].cantidad").value(1));

        mockMvc.perform(get("/api/v1/reportes/citas/tendencia")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .param("granularidad", GranularidadReporte.DIARIA.name())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidad").isNumber());

        mockMvc.perform(get("/api/v1/reportes/servicios/mas-solicitados")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()).param("limit", "1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Servicio Principal"))
                .andExpect(jsonPath("$[0].cantidad").value(1));

        mockMvc.perform(get("/api/v1/reportes/vacunas/resumen")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VETERINARIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAplicadas").value(1))
                .andExpect(jsonPath("$.masAplicadas[0].nombre").value("Vacuna Historica"));

        mockMvc.perform(get("/api/v1/reportes/comprobantes/resumen")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobantesEmitidos").value(1))
                .andExpect(jsonPath("$.boletas").value(1))
                .andExpect(jsonPath("$.facturas").value(0));
    }

    @Test
    void excludesAppointmentExactlyAtUpperBoundaryAndValidatesRangesAndLimits() throws Exception {
        Cita boundary = nuevaCita(EstadoCita.ATENDIDA, TipoCita.MEDICA, hasta, fixture.mascota, fixture.veterinario);
        agregarServicio(boundary, fixture.servicio, 10);

        mockMvc.perform(get("/api/v1/reportes/citas/resumen")
                        .param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/v1/reportes/servicios/mas-solicitados")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()).param("limit", "101")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reportes/citas/tendencia")
                        .param("desde", hasta.toString()).param("hasta", desde.toString()).param("granularidad", "DIARIA")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardAndFinancialReportsRejectUnauthorizedRoles() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/resumen").param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citas.total").value(3))
                .andExpect(jsonPath("$.ingresosCobrados").value(150.0))
                .andExpect(jsonPath("$.saldoPendiente").value(70.0))
                .andExpect(jsonPath("$.comprobantesEmitidos").value(1));
        mockMvc.perform(get("/api/v1/dashboard/resumen").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/resumen").param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VETERINARIO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes/finanzas/resumen").param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VETERINARIO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes/vacunas/resumen").param("desde", desde.toString()).param("hasta", hasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyPeriodReturnsZerosAndEmptyCollections() throws Exception {
        LocalDateTime emptyDesde = hasta.plusDays(10);
        LocalDateTime emptyHasta = emptyDesde.plusDays(1);
        mockMvc.perform(get("/api/v1/reportes/finanzas/resumen").param("desde", emptyDesde.toString()).param("hasta", emptyHasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ingresoTotal").value(0)).andExpect(jsonPath("$.cantidadPagos").value(0));
        mockMvc.perform(get("/api/v1/reportes/servicios/mas-solicitados").param("desde", emptyDesde.toString()).param("hasta", emptyHasta.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private Fixture crearFixture() {
        Usuario veterinario = usuario("vet" + System.nanoTime() + "@test.dev");
        Usuario recepcionista = usuario("recep" + System.nanoTime() + "@test.dev");
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("Ana"); cliente.setPrimerApellido("Reporte"); cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento(String.valueOf(80000000 + (int) (Math.random() * 999999))); cliente.setActivo(true);
        cliente = clientes.saveAndFlush(cliente);
        Mascota mascota = new Mascota(); mascota.setCliente(cliente); mascota.setNombre("Luna"); mascota.setEspecie(EspecieMascota.PERRO); mascota.setActivo(true);
        mascota = mascotas.saveAndFlush(mascota);
        Servicio servicio = servicio("Servicio Principal", TipoServicio.MEDICO, 100);
        Servicio otro = servicio("Servicio Secundario", TipoServicio.PELUQUERIA, 120);
        Cita citaA = nuevaCita(EstadoCita.ATENDIDA, TipoCita.MEDICA, desde.plusHours(2), mascota, veterinario); agregarServicio(citaA, servicio, 100);
        Cita citaB = nuevaCita(EstadoCita.ATENDIDA, TipoCita.PELUQUERIA, desde.plusHours(3), mascota, veterinario); agregarServicio(citaB, otro, 120);
        Cita citaCancelada = nuevaCita(EstadoCita.CANCELADA, TipoCita.PELUQUERIA, desde.plusHours(4), mascota, veterinario); citaCancelada.setMotivoCancelacion("fixture"); citas.saveAndFlush(citaCancelada);
        Pago pagoA1 = pago(citaA, 40, EstadoPago.PAGADO); detalle(pagoA1, MedioPago.EFECTIVO, 40); comprobante(pagoA1, EstadoComprobante.EMITIDO);
        Pago pagoA2 = pago(citaA, 60, EstadoPago.PAGADO); detalle(pagoA2, MedioPago.EFECTIVO, 50); detalle(pagoA2, MedioPago.YAPE, 10);
        Pago pagoB = pago(citaB, 50, EstadoPago.PAGADO); detalle(pagoB, MedioPago.YAPE, 50);
        pago(citaB, 1, EstadoPago.PENDIENTE); pago(citaB, 2, EstadoPago.ANULADO);
        AtencionMedica atencion = new AtencionMedica(); atencion.setCita(citaA); atencion.setVeterinario(veterinario); atencion.setDiagnostico("diagnostico"); atencion.setTratamiento("tratamiento"); atencion.setReceta("receta"); atencionesMedicas.saveAndFlush(atencion);
        Vacuna vacuna = new Vacuna(); vacuna.setNombre("Vacuna Historica"); vacuna.setActivo(false); vacunas.saveAndFlush(vacuna);
        VacunaAplicada aplicada = new VacunaAplicada(); aplicada.setAtencionMedica(atencion); aplicada.setVacuna(vacuna); aplicada.setFechaAplicacion(desde.toLocalDate().plusDays(1)); vacunasAplicadas.saveAndFlush(aplicada);
        return new Fixture(veterinario, recepcionista, mascota, servicio);
    }

    private Usuario usuario(String correo) { Usuario u = new Usuario(); u.setPrimerNombre("Test"); u.setPrimerApellido("Reporte"); u.setCorreo(correo); u.setPasswordHash("hash"); u.setActivo(true); u.setIntentosFallidos(0); return usuarios.saveAndFlush(u); }
    private Servicio servicio(String nombre, TipoServicio tipo, int precio) { Servicio s = new Servicio(); s.setNombre(nombre); s.setTipoServicio(tipo); s.setPrecioBase(BigDecimal.valueOf(precio)); s.setDuracionMinutos(30); s.setActivo(true); return servicios.saveAndFlush(s); }
    private Cita nuevaCita(EstadoCita estado, TipoCita tipo, LocalDateTime inicio, Mascota mascota, Usuario trabajador) { Cita c = new Cita(); c.setMascota(mascota); c.setTrabajadorAsignado(trabajador); c.setRegistradoPor(trabajador); c.setTipoCita(tipo); c.setEstado(estado); c.setFechaHoraInicio(inicio); c.setFechaHoraFin(inicio.plusMinutes(30)); c.setMotivoCancelacion("fixture"); c.setMotivoNoAtencion("fixture"); return citas.saveAndFlush(c); }
    private void agregarServicio(Cita cita, Servicio servicio, int precio) { CitaServicio cs = new CitaServicio(); cs.setCita(cita); cs.setServicio(servicio); cs.setPrecioAplicado(BigDecimal.valueOf(precio)); cs.setDuracionAplicadaMinutos(30); citaServicios.saveAndFlush(cs); }
    private Pago pago(Cita cita, int monto, EstadoPago estado) { Pago p = new Pago(); p.setCita(cita); p.setRegistradoPor(fixture == null ? usuarios.findAll().get(0) : fixture.recepcionista); p.setMontoTotal(BigDecimal.valueOf(monto)); p.setEstado(estado); p.setFechaPago(estado == EstadoPago.PAGADO ? desde.plusHours(5) : null); return pagos.saveAndFlush(p); }
    private void detalle(Pago pago, MedioPago medio, int monto) { DetallePago d = new DetallePago(); d.setPago(pago); d.setMedioPago(medio); d.setMonto(BigDecimal.valueOf(monto)); detalles.saveAndFlush(d); }
    private void comprobante(Pago pago, EstadoComprobante estado) { Comprobante c = new Comprobante(); c.setPago(pago); c.setTipoComprobante(TipoComprobante.BOLETA); c.setSerie("B001"); c.setNumero("1"); c.setEstado(estado); comprobantes.saveAndFlush(c); }
    private record Fixture(Usuario veterinario, Usuario recepcionista, Mascota mascota, Servicio servicio) { }
}
