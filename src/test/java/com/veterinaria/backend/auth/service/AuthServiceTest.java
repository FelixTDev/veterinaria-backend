package com.veterinaria.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.veterinaria.backend.auth.dto.CambiarPasswordRequest;
import com.veterinaria.backend.auth.dto.LoginRequest;
import com.veterinaria.backend.auth.dto.LoginResponse;
import com.veterinaria.backend.auth.exception.CredencialesInvalidasException;
import com.veterinaria.backend.auth.exception.CuentaBloqueadaException;
import com.veterinaria.backend.auth.exception.PasswordActualIncorrectaException;
import com.veterinaria.backend.auth.exception.PasswordsNoCoincidenException;
import com.veterinaria.backend.auth.exception.UsuarioInactivoException;
import com.veterinaria.backend.auth.security.JwtTokenService;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private JwtTokenService jwtTokenService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                usuarioRepository,
                usuarioRolRepository,
                passwordEncoder,
                passwordPolicyValidator,
                jwtTokenService);
    }

    @Test
    void shouldLoginSuccessfullyAndResetFailedAttempts() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        usuario.setIntentosFallidos(3);
        usuario.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate("ana@test.dev")).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findActivosByUsuarioId(1L)).thenReturn(List.of(buildUsuarioRol(NombreRol.RECEPCIONISTA)));
        when(jwtTokenService.generateAccessToken(usuario, List.of("RECEPCIONISTA")))
                .thenReturn(new JwtTokenService.TokenResult("jwt-access", 3600));

        LoginResponse response = authService.login(new LoginRequest("ana@test.dev", "Password1!"));

        assertThat(response.accessToken()).isEqualTo("jwt-access");
        assertThat(response.usuario().roles()).containsExactly("RECEPCIONISTA");
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(usuario.getUltimoAcceso()).isNotNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void shouldIncreaseFailedAttemptsWhenPasswordIsIncorrect() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate("ana@test.dev")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.dev", "OtroPassword1!")))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.getBloqueadoHasta()).isNull();
        verify(jwtTokenService, never()).generateAccessToken(any(), any());
    }

    @Test
    void shouldBlockAccountOnFifthFailedAttempt() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        usuario.setIntentosFallidos(4);
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate("ana@test.dev")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.dev", "OtroPassword1!")))
                .isInstanceOf(CuentaBloqueadaException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getBloqueadoHasta()).isAfter(LocalDateTime.now().plusMinutes(14));
    }

    @Test
    void shouldRejectBlockedUser() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate("ana@test.dev")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.dev", "Password1!")))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    void shouldRejectInactiveUser() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        usuario.setActivo(Boolean.FALSE);
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate("ana@test.dev")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.dev", "Password1!")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        authService.changePassword(1L, new CambiarPasswordRequest("Password1!", "NuevoPassword1!", "NuevoPassword1!"));

        assertThat(passwordEncoder.matches("NuevoPassword1!", usuario.getPasswordHash())).isTrue();
        verify(passwordPolicyValidator).validate("NuevoPassword1!");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsWrong() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.changePassword(
                1L,
                new CambiarPasswordRequest("Incorrecta", "NuevoPassword1!", "NuevoPassword1!")))
                .isInstanceOf(PasswordActualIncorrectaException.class);
    }

    @Test
    void shouldRejectPasswordChangeWhenConfirmationDoesNotMatch() {
        Usuario usuario = buildUsuario("ana@test.dev", "Password1!");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.changePassword(
                1L,
                new CambiarPasswordRequest("Password1!", "NuevoPassword1!", "Diferente1!")))
                .isInstanceOf(PasswordsNoCoincidenException.class);
    }

    private Usuario buildUsuario(String correo, String rawPassword) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPrimerNombre("Ana");
        usuario.setPrimerApellido("Torres");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(rawPassword));
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        return usuario;
    }

    private UsuarioRol buildUsuarioRol(NombreRol nombreRol) {
        Rol rol = new Rol();
        rol.setNombre(nombreRol);
        rol.setActivo(Boolean.TRUE);
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setRol(rol);
        return usuarioRol;
    }
}
