package com.veterinaria.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.veterinaria.backend.auth.entity.CodigoRecuperacion;
import com.veterinaria.backend.auth.repository.CodigoRecuperacionRepository;
import com.veterinaria.backend.auth.service.CorreoService;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTest.FakeMailConfiguration.class)
class AuthIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioRolRepository usuarioRolRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeCorreoService fakeCorreoService;

    @BeforeEach
    void cleanUp() {
        codigoRecuperacionRepository.deleteAll();
        usuarioRolRepository.deleteAll();
        usuarioRepository.deleteAll();
        fakeCorreoService.clear();
    }

    @Test
    void shouldLoginAndAccessMeWithValidJwt() throws Exception {
        Usuario usuario = createUsuarioConRoles("trabajador@test.dev", "Password1!", "RECEPCIONISTA", "PELUQUERO");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Password1!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.roles[0]").exists())
                .andReturn();

        String accessToken = extractJsonValue(loginResult.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(usuario.getCorreo()))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectSameAccessTokenAfterUserIsDeactivated() throws Exception {
        Usuario usuario = createUsuarioConRoles("revocado@test.dev", "Password1!", "RECEPCIONISTA");
        String accessToken = extractJsonValue(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Password1!")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotRevealInactiveAccountDuringLogin() throws Exception {
        Usuario usuario = createUsuarioConRoles("inactivo@test.dev", "Password1!", "RECEPCIONISTA");
        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Password1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales invalidas."));
    }

    @Test
    void shouldAllowAngularCorsPreflightWithAuthorization() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void shouldPersistBlockingAfterFifthFailedAttempt() throws Exception {
        Usuario usuario = createUsuarioConRoles("bloqueo@test.dev", "Password1!", "RECEPCIONISTA");

        for (int index = 0; index < 4; index++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(usuario.getCorreo(), "Incorrecta1!")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Incorrecta1!")))
                .andExpect(status().isLocked());

        Usuario updated = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(updated.getIntentosFallidos()).isEqualTo(5);
        assertThat(updated.getBloqueadoHasta()).isAfter(LocalDateTime.now().plusMinutes(14));
    }

    @Test
    void shouldResetFailedAttemptsAfterSuccessfulLogin() throws Exception {
        Usuario usuario = createUsuarioConRoles("reset@test.dev", "Password1!", "RECEPCIONISTA");
        usuario.setIntentosFallidos(4);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Password1!")))
                .andExpect(status().isOk());

        Usuario updated = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(updated.getIntentosFallidos()).isZero();
        assertThat(updated.getBloqueadoHasta()).isNull();
        assertThat(updated.getUltimoAcceso()).isNotNull();
    }

    @Test
    void shouldPersistRecoveryCodeAndResetPasswordWithoutSendingRealMail() throws Exception {
        Usuario usuario = createUsuarioConRoles("recuperacion@test.dev", "Password1!", "RECEPCIONISTA");

        mockMvc.perform(post("/api/v1/auth/recuperacion/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correoJson(usuario.getCorreo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Si el correo esta registrado, recibiras un codigo de recuperacion."));

        assertThat(fakeCorreoService.sentCodes()).hasSize(1);
        assertThat(fakeCorreoService.sentCodes().get(0).codigo()).matches("\\d{6}");
        CodigoRecuperacion codigo = codigoRecuperacionRepository
                .findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(usuario.getId())
                .orElseThrow();
        assertThat(codigo.getUsado()).isFalse();

        MvcResult validarResult = mockMvc.perform(post("/api/v1/auth/recuperacion/validar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validarCodigoJson(usuario.getCorreo(), fakeCorreoService.sentCodes().get(0).codigo())))
                .andExpect(status().isOk())
                .andReturn();

        String recoveryToken = extractJsonValue(validarResult.getResponse().getContentAsString(), "recoveryToken");

        mockMvc.perform(post("/api/v1/auth/recuperacion/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restablecerJson(recoveryToken, "NuevoPassword1!", "NuevoPassword1!")))
                .andExpect(status().isOk());

        Usuario updated = usuarioRepository.findById(usuario.getId()).orElseThrow();
        CodigoRecuperacion usedCode = codigoRecuperacionRepository.findById(codigo.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NuevoPassword1!", updated.getPasswordHash())).isTrue();
        assertThat(usedCode.getUsado()).isTrue();
        assertThat(usedCode.getUsadoEn()).isNotNull();

        mockMvc.perform(post("/api/v1/auth/recuperacion/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restablecerJson(recoveryToken, "OtroPassword1!", "OtroPassword1!")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRecoveryTokenOnProtectedEndpointAndAccessTokenOnReset() throws Exception {
        Usuario usuario = createUsuarioConRoles("purpose@test.dev", "Password1!", "RECEPCIONISTA");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(usuario.getCorreo(), "Password1!")))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = extractJsonValue(loginResult.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(post("/api/v1/auth/recuperacion/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correoJson(usuario.getCorreo())))
                .andExpect(status().isOk());

        MvcResult validarResult = mockMvc.perform(post("/api/v1/auth/recuperacion/validar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validarCodigoJson(usuario.getCorreo(), fakeCorreoService.sentCodes().get(0).codigo())))
                .andExpect(status().isOk())
                .andReturn();
        String recoveryToken = extractJsonValue(validarResult.getResponse().getContentAsString(), "recoveryToken");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + recoveryToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/recuperacion/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restablecerJson(accessToken, "NuevoPassword1!", "NuevoPassword1!")))
                .andExpect(status().isBadRequest());
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

    private String loginJson(String correo, String password) {
        return """
                {"correo":"%s","password":"%s"}
                """.formatted(correo, password);
    }

    private String correoJson(String correo) {
        return """
                {"correo":"%s"}
                """.formatted(correo);
    }

    private String validarCodigoJson(String correo, String codigo) {
        return """
                {"correo":"%s","codigo":"%s"}
                """.formatted(correo, codigo);
    }

    private String restablecerJson(String recoveryToken, String passwordNueva, String confirmacionPassword) {
        return """
                {"recoveryToken":"%s","passwordNueva":"%s","confirmacionPassword":"%s"}
                """.formatted(recoveryToken, passwordNueva, confirmacionPassword);
    }

    private String extractJsonValue(String content, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @TestConfiguration
    static class FakeMailConfiguration {

        @Bean
        @Primary
        FakeCorreoService fakeCorreoService() {
            return new FakeCorreoService();
        }
    }

    static class FakeCorreoService implements CorreoService {

        private final List<SentCode> sentCodes = new ArrayList<>();

        @Override
        public void enviarCodigoRecuperacion(String correoDestino, String nombreCompleto, String codigo) {
            sentCodes.add(new SentCode(correoDestino, nombreCompleto, codigo));
        }

        List<SentCode> sentCodes() {
            return sentCodes;
        }

        void clear() {
            sentCodes.clear();
        }
    }

    record SentCode(String correo, String nombreCompleto, String codigo) {
    }
}
