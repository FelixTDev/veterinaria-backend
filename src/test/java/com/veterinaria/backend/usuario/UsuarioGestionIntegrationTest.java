package com.veterinaria.backend.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
class UsuarioGestionIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioRolRepository usuarioRolRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    void cleanUp() {
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void administradorShouldCreateWorkerWithEncryptedPasswordAndMultipleRoles() throws Exception {
        String token = tokenFor(createUsuarioConRoles("admin@test.dev", "Password1!", "ADMINISTRADOR"));

        MvcResult result = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson(" Ana.Torres@TEST.Dev ", "RECEPCIONISTA", "PELUQUERO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana.torres@test.dev"))
                .andExpect(jsonPath("$.roles[0]").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.roles[1]").value("PELUQUERO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        Usuario saved = usuarioRepository.findByCorreoIgnoreCase("ana.torres@test.dev").orElseThrow();
        assertThat(passwordEncoder.matches("Password123*", saved.getPasswordHash())).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("passwordHash");
    }

    @Test
    void shouldReturnUnauthorizedWithoutTokenAndForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson("ana@test.dev", "RECEPCIONISTA")))
                .andExpect(status().isUnauthorized());

        String recepcionistaToken = tokenFor(createUsuarioConRoles("recepcion@test.dev", "Password1!", "RECEPCIONISTA"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + recepcionistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson("ana@test.dev", "RECEPCIONISTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDuplicateEmailWithConflict() throws Exception {
        String token = tokenFor(createUsuarioConRoles("admin@test.dev", "Password1!", "ADMINISTRADOR"));
        createUsuarioConRoles("ana@test.dev", "Password1!", "RECEPCIONISTA");

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson("ANA@test.dev", "PELUQUERO")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListWithPaginationSearchStatusAndRoleFilters() throws Exception {
        String token = tokenFor(createUsuarioConRoles("admin@test.dev", "Password1!", "ADMINISTRADOR"));
        createUsuarioConRoles("ana@test.dev", "Password1!", "RECEPCIONISTA");
        Usuario inactive = createUsuarioConRoles("beatriz@test.dev", "Password1!", "PELUQUERO");
        inactive.setActivo(Boolean.FALSE);
        usuarioRepository.save(inactive);

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "ana")
                        .param("activo", "true")
                        .param("rol", "RECEPCIONISTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].correo").value("ana@test.dev"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void shouldGetUpdateChangeStateAndKeepUserPersisted() throws Exception {
        String token = tokenFor(createUsuarioConRoles("admin@test.dev", "Password1!", "ADMINISTRADOR"));
        Usuario worker = createUsuarioConRoles("ana@test.dev", "Password1!", "RECEPCIONISTA");

        mockMvc.perform(get("/api/v1/usuarios/{id}", worker.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@test.dev"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(put("/api/v1/usuarios/{id}", worker.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primerNombre":"Ana Maria",
                                  "segundoNombre":null,
                                  "primerApellido":"Torres",
                                  "segundoApellido":"Lopez",
                                  "correo":" ANA.NUEVA@Test.Dev ",
                                  "telefono":"999111222"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana.nueva@test.dev"))
                .andExpect(jsonPath("$.nombreCompleto").value("Ana Maria Torres Lopez"));

        mockMvc.perform(patch("/api/v1/usuarios/{id}/estado", worker.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        assertThat(usuarioRepository.existsById(worker.getId())).isTrue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ana.nueva@test.dev", "Password1!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReplaceRolesAndProtectLastAdministrator() throws Exception {
        Usuario admin = createUsuarioConRoles("admin@test.dev", "Password1!", "ADMINISTRADOR");
        String token = tokenFor(admin);
        Usuario worker = createUsuarioConRoles("ana@test.dev", "Password1!", "RECEPCIONISTA");

        mockMvc.perform(put("/api/v1/usuarios/{id}/roles", worker.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"VETERINARIO\",\"PELUQUERO\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("VETERINARIO"))
                .andExpect(jsonPath("$.roles[1]").value("PELUQUERO"));

        mockMvc.perform(put("/api/v1/usuarios/{id}/roles", admin.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"RECEPCIONISTA\"]}"))
                .andExpect(status().isConflict());

        String syntheticAdminToken = tokenFor(admin);
        mockMvc.perform(patch("/api/v1/usuarios/{id}/estado", admin.getId())
                        .header("Authorization", "Bearer " + syntheticAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListActiveRoles() throws Exception {
        String token = tokenFor(createUsuarioConRoles("admin@test.dev", "Password1!", "ADMINISTRADOR"));

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'ADMINISTRADOR')]").exists())
                .andExpect(jsonPath("$[?(@.nombre == 'RECEPCIONISTA')]").exists())
                .andExpect(jsonPath("$[?(@.nombre == 'VETERINARIO')]").exists())
                .andExpect(jsonPath("$[?(@.nombre == 'PELUQUERO')]").exists());
    }

    private Usuario createUsuarioConRoles(String correo, String rawPassword, String... roles) {
        Usuario usuario = new Usuario();
        usuario.setPrimerNombre("Ana");
        usuario.setPrimerApellido("Torres");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(rawPassword));
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        Usuario savedUsuario = usuarioRepository.save(usuario);

        for (String roleName : roles) {
            Rol rol = rolRepository.findAll()
                    .stream()
                    .filter(item -> item.getNombre().name().equals(roleName))
                    .findFirst()
                    .orElseThrow();
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(savedUsuario);
            usuarioRol.setRol(rol);
            usuarioRolRepository.save(usuarioRol);
        }
        return savedUsuario;
    }

    private String tokenFor(Usuario usuario) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Password1!")))
                .andExpect(status().isOk())
                .andReturn();
        return extractJsonValue(result.getResponse().getContentAsString(), "accessToken");
    }

    private String crearUsuarioJson(String correo, String... roles) {
        String rolesJson = java.util.Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "primerNombre":"Ana",
                  "segundoNombre":null,
                  "primerApellido":"Torres",
                  "segundoApellido":"Lopez",
                  "correo":"%s",
                  "telefono":"999888777",
                  "passwordInicial":"Password123*",
                  "roles":[%s]
                }
                """.formatted(correo, rolesJson);
    }

    private String loginJson(String correo, String password) {
        return """
                {"correo":"%s","password":"%s"}
                """.formatted(correo, password);
    }

    private String extractJsonValue(String content, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
