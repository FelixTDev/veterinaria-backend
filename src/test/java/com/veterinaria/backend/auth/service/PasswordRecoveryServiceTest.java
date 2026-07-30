package com.veterinaria.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.veterinaria.backend.auth.dto.RestablecerPasswordRequest;
import com.veterinaria.backend.auth.dto.SolicitarRecuperacionRequest;
import com.veterinaria.backend.auth.dto.ValidarCodigoRequest;
import com.veterinaria.backend.auth.dto.ValidarCodigoResponse;
import com.veterinaria.backend.auth.entity.CodigoRecuperacion;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionExpiradoException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionInvalidoException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionUsadoException;
import com.veterinaria.backend.auth.exception.RecoveryTokenInvalidoException;
import com.veterinaria.backend.auth.security.JwtTokenService;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private com.veterinaria.backend.auth.repository.CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private CorreoService correoService;

    @Mock
    private JwtTokenService jwtTokenService;

    private PasswordEncoder passwordEncoder;
    private PasswordRecoveryService passwordRecoveryService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        AuthMailProperties authMailProperties = new AuthMailProperties();
        authMailProperties.setFrom("test@veterinaria.local");
        authMailProperties.setRecoveryCodeExpirationMinutes(10);
        passwordRecoveryService = new PasswordRecoveryService(
                usuarioRepository,
                codigoRecuperacionRepository,
                passwordEncoder,
                passwordPolicyValidator,
                correoService,
                jwtTokenService,
                authMailProperties);
    }

    @Test
    void shouldGenerateSixDigitRecoveryCodeAndReturnGenericResponse() {
        Usuario usuario = buildUsuario();
        when(usuarioRepository.findByCorreoIgnoreCase("ana@test.dev")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findByUsuario_IdAndUsadoFalse(1L)).thenReturn(List.of());

        var response = passwordRecoveryService.solicitarCodigo(new SolicitarRecuperacionRequest("ana@test.dev"));

        ArgumentCaptor<CodigoRecuperacion> captor = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRecuperacionRepository).save(captor.capture());
        verify(correoService).enviarCodigoRecuperacion(
                org.mockito.ArgumentMatchers.eq("ana@test.dev"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.argThat(code -> code.matches("\\d{6}")));
        assertThat(passwordEncoder.matches(
                extractSentCode(),
                captor.getValue().getCodigoHash())).isTrue();
        assertThat(response.mensaje()).contains("Si el correo esta registrado");
    }

    @Test
    void shouldReturnRecoveryTokenWhenCodeIsValid() {
        Usuario usuario = buildUsuario();
        CodigoRecuperacion codigo = buildCodigo(usuario, "123456");
        when(usuarioRepository.findByCorreoIgnoreCase("ana@test.dev")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(codigo));
        when(jwtTokenService.generateRecoveryToken(usuario, 10L))
                .thenReturn(new JwtTokenService.TokenResult("recovery-token", 600));

        ValidarCodigoResponse response =
                passwordRecoveryService.validarCodigo(new ValidarCodigoRequest("ana@test.dev", "123456"));

        assertThat(response.recoveryToken()).isEqualTo("recovery-token");
        assertThat(response.expiresIn()).isEqualTo(600);
    }

    @Test
    void shouldRejectExpiredCode() {
        Usuario usuario = buildUsuario();
        CodigoRecuperacion codigo = buildCodigo(usuario, "123456");
        codigo.setExpiraEn(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByCorreoIgnoreCase("ana@test.dev")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> passwordRecoveryService.validarCodigo(new ValidarCodigoRequest("ana@test.dev", "123456")))
                .isInstanceOf(CodigoRecuperacionExpiradoException.class);
    }

    @Test
    void shouldRejectUsedCode() {
        Usuario usuario = buildUsuario();
        CodigoRecuperacion codigo = buildCodigo(usuario, "123456");
        codigo.setUsado(true);
        when(usuarioRepository.findByCorreoIgnoreCase("ana@test.dev")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> passwordRecoveryService.validarCodigo(new ValidarCodigoRequest("ana@test.dev", "123456")))
                .isInstanceOf(CodigoRecuperacionUsadoException.class);
    }

    @Test
    void shouldRejectInvalidCode() {
        Usuario usuario = buildUsuario();
        CodigoRecuperacion codigo = buildCodigo(usuario, "123456");
        when(usuarioRepository.findByCorreoIgnoreCase("ana@test.dev")).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> passwordRecoveryService.validarCodigo(new ValidarCodigoRequest("ana@test.dev", "654321")))
                .isInstanceOf(CodigoRecuperacionInvalidoException.class);
    }

    @Test
    void shouldResetPasswordAndMarkCodeAsUsed() {
        Usuario usuario = buildUsuario();
        CodigoRecuperacion codigo = buildCodigo(usuario, "123456");
        Jwt jwt = Jwt.withTokenValue("recovery-token")
                .header("alg", "HS256")
                .claim("uid", 1L)
                .claim("recoveryCodeId", 10L)
                .claim("purpose", "recovery")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(600))
                .subject("ana@test.dev")
                .build();
        when(jwtTokenService.decodeRecoveryToken("recovery-token")).thenReturn(jwt);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(codigoRecuperacionRepository.findByIdAndUsuario_Id(10L, 1L)).thenReturn(Optional.of(codigo));

        passwordRecoveryService.restablecerPassword(
                new RestablecerPasswordRequest("recovery-token", "NuevoPassword1!", "NuevoPassword1!"));

        assertThat(passwordEncoder.matches("NuevoPassword1!", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(codigo.getUsado()).isTrue();
        assertThat(codigo.getUsadoEn()).isNotNull();
    }

    @Test
    void shouldRejectInvalidRecoveryToken() {
        when(jwtTokenService.decodeRecoveryToken("bad-token"))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("bad"));

        assertThatThrownBy(() -> passwordRecoveryService.restablecerPassword(
                new RestablecerPasswordRequest("bad-token", "NuevoPassword1!", "NuevoPassword1!")))
                .isInstanceOf(RecoveryTokenInvalidoException.class);
    }

    private Usuario buildUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPrimerNombre("Ana");
        usuario.setPrimerApellido("Torres");
        usuario.setCorreo("ana@test.dev");
        usuario.setPasswordHash(passwordEncoder.encode("Password1!"));
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(2);
        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(5));
        return usuario;
    }

    private CodigoRecuperacion buildCodigo(Usuario usuario, String rawCode) {
        CodigoRecuperacion codigo = new CodigoRecuperacion();
        codigo.setId(10L);
        codigo.setUsuario(usuario);
        codigo.setCodigoHash(passwordEncoder.encode(rawCode));
        codigo.setExpiraEn(LocalDateTime.now().plusMinutes(10));
        codigo.setUsado(false);
        return codigo;
    }

    private String extractSentCode() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarCodigoRecuperacion(any(), any(), captor.capture());
        return captor.getValue();
    }
}
