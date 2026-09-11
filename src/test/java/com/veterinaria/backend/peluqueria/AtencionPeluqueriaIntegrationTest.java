package com.veterinaria.backend.peluqueria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.repository.CitaServicioRepository;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.peluqueria.entity.FotoPeluqueria;
import com.veterinaria.backend.peluqueria.repository.FotoPeluqueriaRepository;
import com.veterinaria.backend.peluqueria.repository.AtencionPeluqueriaRepository;
import com.veterinaria.backend.peluqueria.storage.ImageStorageService;
import com.veterinaria.backend.peluqueria.storage.StoredImage;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AtencionPeluqueriaIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CitaServicioRepository citaServicioRepository;
    @Autowired private AtencionPeluqueriaRepository atencionRepository;
    @Autowired private FotoPeluqueriaRepository fotoRepository;

    @MockitoBean private ImageStorageService imageStorageService;

    @BeforeEach
    void cleanUp() {
        fotoRepository.deleteAll();
        atencionRepository.deleteAll();
        citaServicioRepository.deleteAll();
        citaRepository.deleteAll();
        mascotaRepository.deleteAll();
        clienteRepository.deleteAll();
        servicioRepository.deleteAll();
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void completesHairdresserFlowWithMockedCloudinaryAndRejectsGenericClose() throws Exception {
        Usuario peluquero = createUser("peluqueria.integration@test.dev", NombreRol.PELUQUERO);
        Cliente cliente = createCliente();
        Mascota mascota = createMascota(cliente);
        Servicio servicio = createServicio();
        Cita cita = createConfirmedAppointment(mascota, peluquero, servicio);
        when(imageStorageService.upload(any(), anyString(), anyString()))
                .thenReturn(new StoredImage("https://cdn.test/secure.jpg", "veterinaria/peluqueria/1/final/uuid"));

        org.springframework.test.web.servlet.MvcResult created = mockMvc.perform(post("/api/v1/citas/{id}/atencion-peluqueria", cita.getId())
                        .with(jwtFor(peluquero, "PELUQUERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"  Servicio completo  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCita").value("CONFIRMADA"))
                .andReturn();
        long atencionId = extractLong(created.getResponse().getContentAsString(), "id");

        mockMvc.perform(multipart("/api/v1/atenciones-peluqueria/{id}/evidencias", atencionId)
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "archivo", "foto.jpg", "image/jpeg", jpegBytes()))
                        .param("tipoFoto", "FINAL")
                        .with(jwtFor(peluquero, "PELUQUERO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlArchivo").value("https://cdn.test/secure.jpg"));

        mockMvc.perform(patch("/api/v1/atenciones-peluqueria/{id}/cerrar", atencionId)
                        .with(jwtFor(peluquero, "PELUQUERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCita").value("ATENDIDA"));

        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.ATENDIDA);
        FotoPeluqueria foto = fotoRepository.findAllByAtencionPeluqueriaIdOrderByCreatedAtAscIdAsc(atencionId).getFirst();
        assertThat(foto.getStorageKey()).isEqualTo("veterinaria/peluqueria/1/final/uuid");

        mockMvc.perform(get("/api/v1/atenciones-peluqueria/{id}/evidencias", atencionId)
                        .with(jwtFor(peluquero, "PELUQUERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoFoto").value("FINAL"));

        mockMvc.perform(patch("/api/v1/citas/{id}/atendida", cita.getId())
                        .with(jwtFor(peluquero, "PELUQUERO")))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotCloseHairdresserAttentionWithoutEvidence() throws Exception {
        Usuario peluquero = createUser("peluqueria.noevidence@test.dev", NombreRol.PELUQUERO);
        Cita cita = createConfirmedAppointment(createMascota(createCliente()), peluquero, createServicio());
        String response = mockMvc.perform(post("/api/v1/citas/{id}/atencion-peluqueria", cita.getId())
                        .with(jwtFor(peluquero, "PELUQUERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long atencionId = extractLong(response, "id");
        mockMvc.perform(patch("/api/v1/atenciones-peluqueria/{id}/cerrar", atencionId)
                        .with(jwtFor(peluquero, "PELUQUERO")))
                .andExpect(status().isConflict());
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.CONFIRMADA);
    }

    private Usuario createUser(String correo, NombreRol rolNombre) {
        Usuario usuario = new Usuario();
        usuario.setPrimerNombre("Pelu");
        usuario.setPrimerApellido("Test");
        usuario.setCorreo(correo);
        usuario.setPasswordHash("not-used");
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        Usuario saved = usuarioRepository.saveAndFlush(usuario);
        Rol rol = rolRepository.findByNombre(rolNombre).orElseThrow();
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(saved);
        usuarioRol.setRol(rol);
        usuarioRolRepository.saveAndFlush(usuarioRol);
        return saved;
    }

    private Cliente createCliente() {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("Cliente");
        cliente.setPrimerApellido("Peluqueria");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("70000001");
        cliente.setActivo(true);
        return clienteRepository.saveAndFlush(cliente);
    }

    private Mascota createMascota(Cliente cliente) {
        Mascota mascota = new Mascota();
        mascota.setCliente(cliente);
        mascota.setNombre("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setActivo(true);
        return mascotaRepository.saveAndFlush(mascota);
    }

    private Servicio createServicio() {
        Servicio servicio = new Servicio();
        servicio.setNombre("Bano integration " + System.nanoTime());
        servicio.setTipoServicio(TipoServicio.PELUQUERIA);
        servicio.setPrecioBase(new BigDecimal("45.00"));
        servicio.setDuracionMinutos(60);
        servicio.setActivo(true);
        return servicioRepository.saveAndFlush(servicio);
    }

    private Cita createConfirmedAppointment(Mascota mascota, Usuario peluquero, Servicio servicio) {
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setTrabajadorAsignado(peluquero);
        cita.setRegistradoPor(peluquero);
        cita.setTipoCita(TipoCita.PELUQUERIA);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(LocalDateTime.now().minusHours(1).withNano(0));
        cita.setFechaHoraFin(LocalDateTime.now().plusHours(1).withNano(0));
        Cita saved = citaRepository.saveAndFlush(cita);
        CitaServicio snapshot = new CitaServicio();
        snapshot.setCita(saved);
        snapshot.setServicio(servicio);
        snapshot.setPrecioAplicado(servicio.getPrecioBase());
        snapshot.setDuracionAplicadaMinutos(servicio.getDuracionMinutos());
        citaServicioRepository.saveAndFlush(snapshot);
        return saved;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Usuario usuario, String role) {
        return jwt().jwt(jwt -> jwt.claim("uid", usuario.getId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private long extractLong(String json, String field) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) throw new AssertionError(field + " missing");
        return Long.parseLong(matcher.group(1));
    }

    private byte[] jpegBytes() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    }

}
