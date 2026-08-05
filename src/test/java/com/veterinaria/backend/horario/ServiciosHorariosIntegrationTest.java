package com.veterinaria.backend.horario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.veterinaria.backend.horario.repository.HorarioTrabajadorRepository;
import com.veterinaria.backend.horario.repository.IndisponibilidadTrabajadorRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiciosHorariosIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private HorarioTrabajadorRepository horarioRepository;
    @Autowired private IndisponibilidadTrabajadorRepository indisponibilidadRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        indisponibilidadRepository.deleteAll();
        horarioRepository.deleteAll();
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void onlyActiveVeterinariansAndGroomersCanBeScheduled() throws Exception {
        Usuario vet = createUser("vet@integration.test", true, "VETERINARIO");
        Usuario peluquero = createUser("pelu@integration.test", true, "PELUQUERO");
        Usuario adminVet = createUser("adminvet@integration.test", true, "ADMINISTRADOR", "VETERINARIO");
        Usuario recepcionista = createUser("recep@integration.test", true, "RECEPCIONISTA");
        Usuario admin = createUser("admin@integration.test", true, "ADMINISTRADOR");
        Usuario inactiveVet = createUser("inactivevet@integration.test", false, "VETERINARIO");
        String adminToken = tokenFor(admin);

        createSchedule(adminToken, vet.getId(), 201);
        createSchedule(adminToken, peluquero.getId(), 201);
        createSchedule(adminToken, adminVet.getId(), 201);
        createSchedule(adminToken, recepcionista.getId(), 409);
        createSchedule(adminToken, admin.getId(), 409);
        createSchedule(adminToken, inactiveVet.getId(), 409);
    }

    @Test
    void ownProgrammingIsVisibleButOtherWorkersAreForbidden() throws Exception {
        Usuario vet = createUser("vet2@integration.test", true, "VETERINARIO");
        Usuario otherVet = createUser("vet3@integration.test", true, "VETERINARIO");
        Usuario peluquero = createUser("pelu2@integration.test", true, "PELUQUERO");
        Usuario recepcionista = createUser("recep2@integration.test", true, "RECEPCIONISTA");
        Usuario admin = createUser("admin2@integration.test", true, "ADMINISTRADOR");
        String adminToken = tokenFor(admin);
        createSchedule(adminToken, vet.getId(), 201);
        createSchedule(adminToken, otherVet.getId(), 201);
        createSchedule(adminToken, peluquero.getId(), 201);

        mockMvc.perform(get("/api/v1/trabajadores/" + vet.getId() + "/horarios")
                        .header("Authorization", "Bearer " + tokenFor(vet)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/trabajadores/" + otherVet.getId() + "/horarios")
                        .header("Authorization", "Bearer " + tokenFor(vet)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/trabajadores/" + peluquero.getId() + "/horarios")
                        .header("Authorization", "Bearer " + tokenFor(peluquero)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/trabajadores/" + vet.getId() + "/horarios")
                        .header("Authorization", "Bearer " + tokenFor(recepcionista)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/trabajadores/" + vet.getId() + "/horarios")
                        .header("Authorization", "Bearer " + tokenFor(recepcionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleJson(2)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/trabajadores/" + vet.getId() + "/horarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogIsReadableByOperationalRolesButWritableOnlyByAdmin() throws Exception {
        Usuario admin = createUser("admin3@integration.test", true, "ADMINISTRADOR");
        Usuario receptionist = createUser("recep3@integration.test", true, "RECEPCIONISTA");
        Usuario vet = createUser("vet4@integration.test", true, "VETERINARIO");
        Usuario peluquero = createUser("pelu3@integration.test", true, "PELUQUERO");
        String body = "{\"nombre\":\"Consulta general\",\"tipoServicio\":\"MEDICO\",\"precioBase\":50,\"duracionMinutos\":30}";
        mockMvc.perform(post("/api/v1/servicios").header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        for (Usuario usuario : new Usuario[] { receptionist, vet, peluquero }) {
            mockMvc.perform(get("/api/v1/servicios").header("Authorization", "Bearer " + tokenFor(usuario)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/servicios").header("Authorization", "Bearer " + tokenFor(receptionist))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }

    private void createSchedule(String token, Long workerId, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/trabajadores/" + workerId + "/horarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleJson(workerId.intValue() % 7 + 1)))
                .andExpect(status().is(expectedStatus));
    }

    private String scheduleJson(int day) {
        return "{\"diaSemana\":" + day + ",\"horaInicio\":\"08:00:00\",\"horaFin\":\"17:00:00\",\"descansoInicio\":\"12:00:00\",\"descansoFin\":\"13:00:00\"}";
    }

    private Usuario createUser(String correo, boolean activo, String... roles) {
        Usuario usuario = new Usuario();
        usuario.setPrimerNombre("Test");
        usuario.setPrimerApellido("User");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("Password1!"));
        usuario.setActivo(activo);
        usuario.setIntentosFallidos(0);
        Usuario saved = usuarioRepository.save(usuario);
        for (String roleName : roles) {
            Rol rol = rolRepository.findAll().stream().filter(item -> item.getNombre().name().equals(roleName)).findFirst().orElseThrow();
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(saved);
            usuarioRol.setRol(rol);
            usuarioRolRepository.save(usuarioRol);
        }
        return saved;
    }

    private String tokenFor(Usuario usuario) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + usuario.getCorreo() + "\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk()).andReturn();
        Matcher matcher = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) throw new AssertionError("accessToken missing");
        return matcher.group(1);
    }
}
