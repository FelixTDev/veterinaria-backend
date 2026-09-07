package com.veterinaria.backend.atencionmedica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AtencionMedicaIntegrationTest extends PostgreSqlContainerConfiguration {
    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private AtencionMedicaRepository atencionRepository;

    @BeforeEach
    void cleanUp() {
        atencionRepository.deleteAll();
        citaRepository.deleteAll();
        mascotaRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void createsAtomicMedicalAttentionAndExposesPagedHistory() throws Exception {
        Usuario vet = createUser("medical.vet@test.dev", "VETERINARIO");
        Usuario receptionist = createUser("medical.reception@test.dev", "RECEPCIONISTA");
        Mascota mascota = createMascota();
        Cita cita = createCita(mascota, vet);

        mockMvc.perform(post("/api/v1/citas/{id}/atencion-medica", cita.getId())
                        .with(jwtFor(vet, "VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citaId").value(cita.getId()))
                .andExpect(jsonPath("$.diagnostico").value("Otitis"));

        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCita.ATENDIDA);
        assertThat(atencionRepository.findByCitaId(cita.getId())).isPresent();

        mockMvc.perform(get("/api/v1/mascotas/{id}/atenciones-medicas", mascota.getId())
                        .with(jwtFor(vet, "VETERINARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].citaId").value(cita.getId()));

        mockMvc.perform(get("/api/v1/citas/{id}/atencion-medica", cita.getId())
                        .with(jwtFor(receptionist, "RECEPCIONISTA")))
                .andExpect(status().isForbidden());
    }

    private Usuario createUser(String correo, String roleName) {
        Usuario user = new Usuario();
        user.setPrimerNombre("Medical"); user.setPrimerApellido("User"); user.setCorreo(correo);
        user.setPasswordHash("hash"); user.setActivo(true); user.setIntentosFallidos(0);
        user = usuarioRepository.save(user);
        Rol role = rolRepository.findAll().stream().filter(r -> r.getNombre().name().equals(roleName)).findFirst().orElseThrow();
        UsuarioRol userRole = new UsuarioRol(); userRole.setUsuario(user); userRole.setRol(role); usuarioRolRepository.save(userRole);
        return user;
    }

    private Mascota createMascota() {
        Cliente cliente = new Cliente(); cliente.setPrimerNombre("Cliente"); cliente.setPrimerApellido("Medical");
        cliente.setTipoDocumento(TipoDocumento.DNI); cliente.setNumeroDocumento("70000001"); cliente.setActivo(true);
        cliente = clienteRepository.save(cliente);
        Mascota mascota = new Mascota(); mascota.setCliente(cliente); mascota.setNombre("Luna");
        mascota.setEspecie(EspecieMascota.PERRO); mascota.setActivo(true); return mascotaRepository.save(mascota);
    }

    private Cita createCita(Mascota mascota, Usuario vet) {
        Cita cita = new Cita(); cita.setMascota(mascota); cita.setTrabajadorAsignado(vet); cita.setRegistradoPor(vet);
        cita.setTipoCita(TipoCita.MEDICA); cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(LocalDateTime.now().minusMinutes(30).withSecond(0).withNano(0));
        cita.setFechaHoraFin(LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0)); return citaRepository.save(cita);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Usuario user, String role) {
        return jwt().jwt(jwt -> jwt.claim("uid", user.getId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String validJson() {
        return "{\"diagnostico\":\" Otitis \",\"tratamiento\":\"Gotas\",\"receta\":\"Receta\"}";
    }
}
