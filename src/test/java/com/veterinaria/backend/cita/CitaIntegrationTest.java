package com.veterinaria.backend.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.repository.CitaServicioRepository;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.horario.entity.HorarioTrabajador;
import com.veterinaria.backend.horario.entity.IndisponibilidadTrabajador;
import com.veterinaria.backend.horario.repository.HorarioTrabajadorRepository;
import com.veterinaria.backend.horario.repository.IndisponibilidadTrabajadorRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CitaIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private PrecioServicioTamanoRepository precioServicioTamanoRepository;
    @Autowired private HorarioTrabajadorRepository horarioRepository;
    @Autowired private IndisponibilidadTrabajadorRepository indisponibilidadRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CitaServicioRepository citaServicioRepository;

    @BeforeEach
    void cleanUp() {
        citaServicioRepository.deleteAll();
        citaRepository.deleteAll();
        indisponibilidadRepository.deleteAll();
        horarioRepository.deleteAll();
        precioServicioTamanoRepository.deleteAll();
        mascotaRepository.deleteAll();
        clienteRepository.deleteAll();
        servicioRepository.deleteAll();
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void shouldEnforceJwtRolesOwnershipAndRegisteredByAcrossRealHttpAndPostgres() throws Exception {
        Usuario recepcionista = createUser("citas.recepcion@test.dev", "RECEPCIONISTA");
        Usuario veterinario = createUser("citas.vet@test.dev", "VETERINARIO");
        Usuario otroVeterinario = createUser("citas.otrovet@test.dev", "VETERINARIO");
        Usuario peluquero = createUser("citas.peluquero@test.dev", "PELUQUERO");
        Cliente cliente = createCliente();
        Mascota mascota = createMascota(cliente);
        Servicio servicio = createServicio();
        Servicio servicioPeluqueria = createServicioPeluqueria();
        LocalDateTime inicio = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        createSchedule(veterinario, inicio);
        createSchedule(peluquero, inicio);
        String recepcionToken = tokenFor(recepcionista);
        String veterinarioToken = tokenFor(veterinario);
        String otroVeterinarioToken = tokenFor(otroVeterinario);

        String body = crearJson(mascota.getId(), veterinario.getId(), servicio.getId(), inicio);
        mockMvc.perform(post("/api/v1/citas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + veterinarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.registradoPor.id").value(recepcionista.getId()))
                .andExpect(jsonPath("$.trabajador.id").value(veterinario.getId()))
                .andReturn();

        long citaId = extractLong(created.getResponse().getContentAsString(), "id");
        CitaServicio snapshot = citaServicioRepository.findByCitaIdIn(java.util.List.of(citaId)).getFirst();
        assertThat(snapshot.getPrecioAplicado()).isEqualByComparingTo("65.00");
        assertThat(snapshot.getDuracionAplicadaMinutos()).isEqualTo(30);

        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", veterinario.getId().toString())
                        .param("tipoCita", "MEDICA")
                        .param("inicio", inicio.plusMinutes(10).toString())
                        .param("fin", inicio.plusMinutes(20).toString())
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidadBase").value(true))
                .andExpect(jsonPath("$.tieneCitaSolapada").value(true))
                .andExpect(jsonPath("$.disponible").value(false));
        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", veterinario.getId().toString())
                        .param("tipoCita", "MEDICA")
                        .param("inicio", inicio.plusMinutes(30).toString())
                        .param("fin", inicio.plusMinutes(45).toString())
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneCitaSolapada").value(false))
                .andExpect(jsonPath("$.disponible").value(true));

        LocalDateTime inicioPeluqueria = inicio.plusHours(1);
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearJson(mascota.getId(), peluquero.getId(), servicioPeluqueria.getId(), inicioPeluqueria)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoCita").value("PELUQUERIA"));
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearMixtaJson(
                                mascota.getId(), veterinario.getId(), servicio.getId(), servicioPeluqueria.getId(), inicio.plusDays(1))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/citas")
                        .header("Authorization", "Bearer " + veterinarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(citaId));
        mockMvc.perform(get("/api/v1/citas")
                        .param("estado", "PENDIENTE")
                        .param("tipoCita", "MEDICA")
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(citaId));
        mockMvc.perform(get("/api/v1/citas/{id}", citaId)
                        .header("Authorization", "Bearer " + otroVeterinarioToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/citas/{id}/atendida", citaId)
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isForbidden());

        LocalDateTime reprogramada = inicio.plusHours(2);
        mockMvc.perform(patch("/api/v1/citas/{id}/reprogramar", citaId)
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trabajadorId\":" + veterinario.getId()
                                + ",\"fechaHoraInicio\":\"" + reprogramada + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaHoraFin").value(
                        reprogramada.plusMinutes(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))));

        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", veterinario.getId().toString())
                        .param("tipoCita", "MEDICA")
                        .param("inicio", inicio.toString())
                        .param("fin", inicio.plusMinutes(30).toString())
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));

        MvcResult cancelable = mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearJson(mascota.getId(), veterinario.getId(), servicio.getId(), inicio.plusHours(4))))
                .andExpect(status().isCreated())
                .andReturn();
        long cancelableId = extractLong(cancelable.getResponse().getContentAsString(), "id");
        mockMvc.perform(patch("/api/v1/citas/{id}/cancelar", cancelableId)
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cliente solicito cancelacion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", veterinario.getId().toString())
                        .param("tipoCita", "MEDICA")
                        .param("inicio", inicio.plusHours(4).toString())
                        .param("fin", inicio.plusHours(4).plusMinutes(30).toString())
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneCitaSolapada").value(false))
                .andExpect(jsonPath("$.disponible").value(true));

        createUnavailability(veterinario, inicio.plusHours(5), inicio.plusHours(6));
        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", veterinario.getId().toString())
                        .param("tipoCita", "MEDICA")
                        .param("inicio", inicio.plusHours(5).toString())
                        .param("fin", inicio.plusHours(5).plusMinutes(30).toString())
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidadBase").value(false))
                .andExpect(jsonPath("$.disponible").value(false));

        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", veterinario.getId().toString())
                        .param("tipoCita", "MEDICA")
                        .param("inicio", inicio.plusHours(3).toString())
                        .param("fin", inicio.plusHours(3).plusMinutes(30).toString())
                        .header("Authorization", "Bearer " + recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidadBase").value(false))
                .andExpect(jsonPath("$.disponible").value(false));

        Usuario veterinarioInactivo = createUser("citas.vet.inactivo@test.dev", "VETERINARIO");
        veterinarioInactivo.setActivo(Boolean.FALSE);
        usuarioRepository.saveAndFlush(veterinarioInactivo);
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearJson(
                                mascota.getId(), veterinarioInactivo.getId(), servicio.getId(), inicio.plusDays(1))))
                .andExpect(status().isConflict());

        Cita cita = citaRepository.findById(citaId).orElseThrow();
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(LocalDateTime.now().minusHours(1).withNano(0));
        cita.setFechaHoraFin(LocalDateTime.now().minusMinutes(30).withNano(0));
        citaRepository.saveAndFlush(cita);

        mockMvc.perform(patch("/api/v1/citas/{id}/atendida", citaId)
                        .header("Authorization", "Bearer " + veterinarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));

        assertThat(citaRepository.findById(citaId).orElseThrow().getEstado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    private Usuario createUser(String correo, String... roles) {
        Usuario usuario = new Usuario();
        usuario.setPrimerNombre("Usuario");
        usuario.setPrimerApellido("Citas");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("Password1!"));
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        Usuario saved = usuarioRepository.save(usuario);
        for (String roleName : roles) {
            Rol rol = rolRepository.findAll().stream()
                    .filter(item -> item.getNombre().name().equals(roleName))
                    .findFirst()
                    .orElseThrow();
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(saved);
            usuarioRol.setRol(rol);
            usuarioRolRepository.save(usuarioRol);
        }
        return saved;
    }

    private Cliente createCliente() {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("Cliente");
        cliente.setPrimerApellido("Citas");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("80909090");
        cliente.setActivo(Boolean.TRUE);
        return clienteRepository.save(cliente);
    }

    private Mascota createMascota(Cliente cliente) {
        Mascota mascota = new Mascota();
        mascota.setCliente(cliente);
        mascota.setNombre("Luna Citas");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setActivo(Boolean.TRUE);
        return mascotaRepository.save(mascota);
    }

    private Servicio createServicio() {
        Servicio servicio = new Servicio();
        servicio.setNombre("Consulta citas integration");
        servicio.setTipoServicio(TipoServicio.MEDICO);
        servicio.setPrecioBase(new BigDecimal("65.00"));
        servicio.setDuracionMinutos(30);
        servicio.setActivo(Boolean.TRUE);
        return servicioRepository.save(servicio);
    }

    private Servicio createServicioPeluqueria() {
        Servicio servicio = new Servicio();
        servicio.setNombre("Bano citas integration");
        servicio.setTipoServicio(TipoServicio.PELUQUERIA);
        servicio.setPrecioBase(new BigDecimal("45.00"));
        servicio.setDuracionMinutos(40);
        servicio.setActivo(Boolean.TRUE);
        return servicioRepository.save(servicio);
    }

    private void createSchedule(Usuario usuario, LocalDateTime inicio) {
        HorarioTrabajador horario = new HorarioTrabajador();
        horario.setUsuario(usuario);
        horario.setDiaSemana(inicio.getDayOfWeek().getValue());
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(18, 0));
        horario.setDescansoInicio(LocalTime.of(13, 0));
        horario.setDescansoFin(LocalTime.of(14, 0));
        horario.setDisponible(Boolean.TRUE);
        horarioRepository.save(horario);
    }

    private void createUnavailability(Usuario usuario, LocalDateTime inicio, LocalDateTime fin) {
        IndisponibilidadTrabajador indisponibilidad = new IndisponibilidadTrabajador();
        indisponibilidad.setUsuario(usuario);
        indisponibilidad.setFechaInicio(inicio);
        indisponibilidad.setFechaFin(fin);
        indisponibilidad.setMotivo("Capacitacion");
        indisponibilidadRepository.save(indisponibilidad);
    }

    private String tokenFor(Usuario usuario) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + usuario.getCorreo() + "\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("accessToken missing");
        }
        return matcher.group(1);
    }

    private long extractLong(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError(field + " missing");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String crearJson(Long mascotaId, Long trabajadorId, Long servicioId, LocalDateTime inicio) {
        return """
                {
                  "mascotaId": %d,
                  "trabajadorId": %d,
                  "fechaHoraInicio": "%s",
                  "servicios": [{"servicioId": %d}],
                  "motivoConsulta": "Control de integracion"
                }
                """.formatted(mascotaId, trabajadorId, inicio, servicioId);
    }

    private String crearMixtaJson(
            Long mascotaId,
            Long trabajadorId,
            Long servicioMedicoId,
            Long servicioPeluqueriaId,
            LocalDateTime inicio) {
        return """
                {
                  "mascotaId": %d,
                  "trabajadorId": %d,
                  "fechaHoraInicio": "%s",
                  "servicios": [{"servicioId": %d}, {"servicioId": %d}]
                }
                """.formatted(mascotaId, trabajadorId, inicio, servicioMedicoId, servicioPeluqueriaId);
    }
}
