package com.veterinaria.backend.vacuna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.atencionmedica.repository.AtencionMedicaRepository;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import com.veterinaria.backend.vacuna.entity.Vacuna;
import com.veterinaria.backend.vacuna.entity.VacunaAplicada;
import com.veterinaria.backend.vacuna.repository.VacunaAplicadaRepository;
import com.veterinaria.backend.vacuna.repository.VacunaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VacunaIntegrationTest extends PostgreSqlContainerConfiguration {
    @Autowired private MockMvc mockMvc;
    @Autowired private VacunaRepository vacunaRepository;
    @Autowired private VacunaAplicadaRepository aplicadaRepository;
    @Autowired private AtencionMedicaRepository atencionRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private RolRepository rolRepository;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void cleanUp() {
        aplicadaRepository.deleteAll(); atencionRepository.deleteAll(); citaRepository.deleteAll();
        mascotaRepository.deleteAll(); clienteRepository.deleteAll(); usuarioRolRepository.deleteAll();
        vacunaRepository.deleteAll(); usuarioRepository.deleteAll();
    }

    @Test
    void createsCatalogAndAppliesVaccineWithDerivedDate() throws Exception {
        Usuario vet = createUser("vaccine.vet@test.dev", "VETERINARIO");
        String body = mockMvc.perform(post("/api/v1/vacunas").with(jwtFor(vet, "ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  Rabia  \",\"descripcion\":\"  Anual  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Rabia"))
                .andReturn().getResponse().getContentAsString();
        long vacunaId = Long.parseLong(body.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
        Vacuna vacuna = vacunaRepository.findById(vacunaId).orElseThrow();
        Mascota mascota = createMascota();
        Cita cita = createCita(mascota, vet);
        AtencionMedica atencion = new AtencionMedica();
        atencion.setCita(cita); atencion.setVeterinario(vet); atencion.setDiagnostico("Sano");
        atencion.setTratamiento("Ninguno"); atencion.setReceta("Ninguna");
        atencion = atencionRepository.saveAndFlush(atencion);
        entityManager.clear();
        atencion = atencionRepository.findByIdForVacunacion(atencion.getId()).orElseThrow();

        mockMvc.perform(post("/api/v1/atenciones-medicas/{id}/vacunas", atencion.getId())
                .with(jwtFor(vet, "VETERINARIO")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"vacunaId\":" + vacuna.getId() + ",\"proximaFecha\":\"" + atencion.getFechaAtencion().toLocalDate() + "\",\"lote\":\"  L-1  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fechaAplicacion").value(atencion.getFechaAtencion().toLocalDate().toString()))
                .andExpect(jsonPath("$.lote").value("L-1"));

        assertThat(aplicadaRepository.findByAtencionMedicaIdOrderByFechaAplicacionDescIdDesc(atencion.getId())).hasSize(1);
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    private Usuario createUser(String correo, String roleName) {
        Usuario user = new Usuario(); user.setPrimerNombre("Vaccine"); user.setPrimerApellido("User"); user.setCorreo(correo);
        user.setPasswordHash("hash"); user.setActivo(true); user.setIntentosFallidos(0); user = usuarioRepository.saveAndFlush(user);
        Rol role = rolRepository.findAll().stream().filter(r -> r.getNombre().name().equals(roleName)).findFirst().orElseThrow();
        UsuarioRol ur = new UsuarioRol(); ur.setUsuario(user); ur.setRol(role); usuarioRolRepository.saveAndFlush(ur); return user;
    }
    private Mascota createMascota() {
        Cliente c = new Cliente(); c.setPrimerNombre("Cliente"); c.setPrimerApellido("Vaccine"); c.setTipoDocumento(TipoDocumento.DNI); c.setNumeroDocumento("79990001"); c.setActivo(true); c = clienteRepository.saveAndFlush(c);
        Mascota m = new Mascota(); m.setCliente(c); m.setNombre("Luna"); m.setEspecie(EspecieMascota.PERRO); m.setActivo(true); return mascotaRepository.saveAndFlush(m);
    }
    private Cita createCita(Mascota mascota, Usuario vet) {
        Cita c = new Cita(); c.setMascota(mascota); c.setTrabajadorAsignado(vet); c.setRegistradoPor(vet); c.setTipoCita(TipoCita.MEDICA); c.setEstado(EstadoCita.ATENDIDA); c.setFechaHoraInicio(LocalDateTime.now().minusHours(1).withSecond(0).withNano(0)); c.setFechaHoraFin(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)); return citaRepository.saveAndFlush(c);
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Usuario u, String role) { return jwt().jwt(j -> j.claim("uid", u.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_" + role)); }
}
