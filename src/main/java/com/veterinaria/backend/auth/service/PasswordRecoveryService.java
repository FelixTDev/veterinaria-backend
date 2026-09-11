package com.veterinaria.backend.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.auth.dto.MensajeResponse;
import com.veterinaria.backend.auth.dto.RestablecerPasswordRequest;
import com.veterinaria.backend.auth.dto.SolicitarRecuperacionRequest;
import com.veterinaria.backend.auth.dto.ValidarCodigoRequest;
import com.veterinaria.backend.auth.dto.ValidarCodigoResponse;
import com.veterinaria.backend.auth.entity.CodigoRecuperacion;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionExpiradoException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionInvalidoException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionUsadoException;
import com.veterinaria.backend.auth.exception.PasswordsNoCoincidenException;
import com.veterinaria.backend.auth.exception.RecoveryTokenInvalidoException;
import com.veterinaria.backend.auth.security.JwtTokenService;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {

    private static final String GENERIC_RECOVERY_MESSAGE =
            "Si el correo esta registrado, recibiras un codigo de recuperacion.";

    private final SecureRandom secureRandom = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final com.veterinaria.backend.auth.repository.CodigoRecuperacionRepository codigoRecuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final CorreoService correoService;
    private final JwtTokenService jwtTokenService;
    private final AuthMailProperties authMailProperties;

    public PasswordRecoveryService(
            UsuarioRepository usuarioRepository,
            com.veterinaria.backend.auth.repository.CodigoRecuperacionRepository codigoRecuperacionRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            CorreoService correoService,
            JwtTokenService jwtTokenService,
            AuthMailProperties authMailProperties) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRecuperacionRepository = codigoRecuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.correoService = correoService;
        this.jwtTokenService = jwtTokenService;
        this.authMailProperties = authMailProperties;
    }

    @Transactional
    public MensajeResponse solicitarCodigo(SolicitarRecuperacionRequest request) {
        usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .ifPresent(this::generateAndSendCode);

        return new MensajeResponse(GENERIC_RECOVERY_MESSAGE);
    }

    @Transactional(readOnly = true)
    public ValidarCodigoResponse validarCodigo(ValidarCodigoRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .orElseThrow(() -> new CodigoRecuperacionInvalidoException("El codigo de recuperacion es invalido."));

        CodigoRecuperacion codigoRecuperacion = codigoRecuperacionRepository
                .findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(usuario.getId())
                .orElseThrow(() -> new CodigoRecuperacionInvalidoException("El codigo de recuperacion es invalido."));

        validateRecoveryCode(codigoRecuperacion, request.codigo());

        JwtTokenService.TokenResult tokenResult =
                jwtTokenService.generateRecoveryToken(usuario, codigoRecuperacion.getId());

        return new ValidarCodigoResponse(tokenResult.tokenValue(), "Bearer", tokenResult.expiresIn());
    }

    @Transactional
    public MensajeResponse restablecerPassword(RestablecerPasswordRequest request) {
        if (!request.passwordNueva().equals(request.confirmacionPassword())) {
            throw new PasswordsNoCoincidenException("La nueva password y su confirmacion no coinciden.");
        }
        passwordPolicyValidator.validate(request.passwordNueva());

        Jwt jwt = decodeRecoveryToken(request.recoveryToken());
        Long userId = jwt.getClaim("uid");
        Long recoveryCodeId = jwt.getClaim("recoveryCodeId");

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecoveryTokenInvalidoException("El recovery token es invalido o expiro."));
        CodigoRecuperacion codigoRecuperacion = codigoRecuperacionRepository
                .findByIdAndUsuarioIdForUpdate(recoveryCodeId, userId)
                .orElseThrow(() -> new RecoveryTokenInvalidoException("El recovery token es invalido o expiro."));

        if (Boolean.TRUE.equals(codigoRecuperacion.getUsado())) {
            throw new CodigoRecuperacionUsadoException("El codigo de recuperacion ya fue utilizado.");
        }
        if (codigoRecuperacion.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new CodigoRecuperacionExpiradoException("El codigo de recuperacion ya expiro.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        codigoRecuperacion.setUsado(true);
        codigoRecuperacion.setUsadoEn(LocalDateTime.now());
        codigoRecuperacionRepository.save(codigoRecuperacion);

        return new MensajeResponse("La password fue restablecida correctamente.");
    }

    private void generateAndSendCode(Usuario usuario) {
        invalidatePreviousCodes(usuario.getId());

        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        CodigoRecuperacion codigoRecuperacion = new CodigoRecuperacion();
        codigoRecuperacion.setUsuario(usuario);
        codigoRecuperacion.setCodigoHash(passwordEncoder.encode(code));
        codigoRecuperacion.setExpiraEn(LocalDateTime.now().plusMinutes(authMailProperties.getRecoveryCodeExpirationMinutes()));
        codigoRecuperacion.setUsado(false);
        codigoRecuperacionRepository.save(codigoRecuperacion);

        correoService.enviarCodigoRecuperacion(usuario.getCorreo(), buildFullName(usuario), code);
    }

    private void invalidatePreviousCodes(Long userId) {
        List<CodigoRecuperacion> codigos = codigoRecuperacionRepository.findByUsuario_IdAndUsadoFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        codigos.forEach(codigo -> {
            codigo.setUsado(true);
            codigo.setUsadoEn(now);
        });
        codigoRecuperacionRepository.saveAll(codigos);
    }

    private void validateRecoveryCode(CodigoRecuperacion codigoRecuperacion, String plainCode) {
        if (Boolean.TRUE.equals(codigoRecuperacion.getUsado())) {
            throw new CodigoRecuperacionUsadoException("El codigo de recuperacion ya fue utilizado.");
        }
        if (codigoRecuperacion.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new CodigoRecuperacionExpiradoException("El codigo de recuperacion ya expiro.");
        }
        if (!passwordEncoder.matches(plainCode, codigoRecuperacion.getCodigoHash())) {
            throw new CodigoRecuperacionInvalidoException("El codigo de recuperacion es invalido.");
        }
    }

    private Jwt decodeRecoveryToken(String token) {
        try {
            return jwtTokenService.decodeRecoveryToken(token);
        } catch (JwtException exception) {
            throw new RecoveryTokenInvalidoException("El recovery token es invalido o expiro.");
        }
    }

    private String buildFullName(Usuario usuario) {
        return java.util.stream.Stream.of(
                        usuario.getPrimerNombre(),
                        usuario.getSegundoNombre(),
                        usuario.getPrimerApellido(),
                        usuario.getSegundoApellido())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse(usuario.getCorreo());
    }
}
