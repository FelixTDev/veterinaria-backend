package com.veterinaria.backend.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.auth.dto.CambiarPasswordRequest;
import com.veterinaria.backend.auth.dto.LoginRequest;
import com.veterinaria.backend.auth.dto.LoginResponse;
import com.veterinaria.backend.auth.dto.MensajeResponse;
import com.veterinaria.backend.auth.dto.UsuarioAutenticadoResponse;
import com.veterinaria.backend.auth.exception.CredencialesInvalidasException;
import com.veterinaria.backend.auth.exception.CuentaBloqueadaException;
import com.veterinaria.backend.auth.exception.PasswordActualIncorrectaException;
import com.veterinaria.backend.auth.exception.PasswordsNoCoincidenException;
import com.veterinaria.backend.auth.exception.UsuarioInactivoException;
import com.veterinaria.backend.auth.security.JwtTokenService;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final int MAX_INTENTOS_FALLIDOS = 5;
    static final int MINUTOS_BLOQUEO = 15;

    private static final String GENERIC_LOGIN_ERROR = "Credenciales invalidas.";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            JwtTokenService jwtTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(noRollbackFor = {
            CredencialesInvalidasException.class,
            CuentaBloqueadaException.class,
            UsuarioInactivoException.class
    })
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCaseForUpdate(request.correo().trim())
                .orElseThrow(() -> new CredencialesInvalidasException(GENERIC_LOGIN_ERROR));

        validateUsuarioAutenticable(usuario);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            handleFailedLogin(usuario);
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        List<String> roles = resolveRoleNames(usuario.getId());
        JwtTokenService.TokenResult token = jwtTokenService.generateAccessToken(usuario, roles);

        return new LoginResponse(
                token.tokenValue(),
                "Bearer",
                token.expiresIn(),
                toUsuarioAutenticadoResponse(usuario, roles));
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticadoResponse getCurrentUser(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario autenticado no encontrado."));
        return toUsuarioAutenticadoResponse(usuario, resolveRoleNames(userId));
    }

    @Transactional
    public MensajeResponse changePassword(Long userId, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario autenticado no encontrado."));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new PasswordActualIncorrectaException("La password actual es incorrecta.");
        }
        if (!request.passwordNueva().equals(request.confirmacionPassword())) {
            throw new PasswordsNoCoincidenException("La nueva password y su confirmacion no coinciden.");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPasswordHash())) {
            throw new PasswordsNoCoincidenException("La nueva password debe ser distinta de la actual.");
        }

        passwordPolicyValidator.validate(request.passwordNueva());
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);

        return new MensajeResponse("La password fue actualizada correctamente.");
    }

    private void validateUsuarioAutenticable(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException(GENERIC_LOGIN_ERROR);
        }
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw new CuentaBloqueadaException("La cuenta se encuentra temporalmente bloqueada.");
        }
        if (usuario.getBloqueadoHasta() != null && !usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
        }
    }

    private void handleFailedLogin(Usuario usuario) {
        int intentosActuales = usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos();
        int nuevosIntentos = intentosActuales + 1;
        usuario.setIntentosFallidos(nuevosIntentos);
        if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
        }
        usuarioRepository.save(usuario);

        if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
            throw new CuentaBloqueadaException("La cuenta se encuentra temporalmente bloqueada.");
        }
        throw new CredencialesInvalidasException(GENERIC_LOGIN_ERROR);
    }

    private List<String> resolveRoleNames(Long userId) {
        return usuarioRolRepository.findActivosByUsuarioId(userId)
                .stream()
                .map(UsuarioRol::getRol)
                .map(rol -> rol.getNombre().name())
                .toList();
    }

    private UsuarioAutenticadoResponse toUsuarioAutenticadoResponse(Usuario usuario, List<String> roles) {
        return new UsuarioAutenticadoResponse(
                usuario.getId(),
                buildFullName(usuario),
                usuario.getCorreo(),
                roles);
    }

    private String buildFullName(Usuario usuario) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, usuario.getPrimerNombre());
        appendIfPresent(builder, usuario.getSegundoNombre());
        appendIfPresent(builder, usuario.getPrimerApellido());
        appendIfPresent(builder, usuario.getSegundoApellido());
        return builder.toString().trim();
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
    }
}
