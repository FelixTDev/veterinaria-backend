package com.veterinaria.backend.auth.controller;

import com.veterinaria.backend.auth.dto.CambiarPasswordRequest;
import com.veterinaria.backend.auth.dto.LoginRequest;
import com.veterinaria.backend.auth.dto.LoginResponse;
import com.veterinaria.backend.auth.dto.MensajeResponse;
import com.veterinaria.backend.auth.dto.RestablecerPasswordRequest;
import com.veterinaria.backend.auth.dto.SolicitarRecuperacionRequest;
import com.veterinaria.backend.auth.dto.UsuarioAutenticadoResponse;
import com.veterinaria.backend.auth.dto.ValidarCodigoRequest;
import com.veterinaria.backend.auth.dto.ValidarCodigoResponse;
import com.veterinaria.backend.auth.service.AuthService;
import com.veterinaria.backend.auth.service.PasswordRecoveryService;

import jakarta.validation.Valid;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse me(JwtAuthenticationToken authentication) {
        return authService.getCurrentUser(authentication.getToken().getClaim("uid"));
    }

    @PostMapping("/cambiar-password")
    public MensajeResponse cambiarPassword(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody CambiarPasswordRequest request) {
        return authService.changePassword(authentication.getToken().getClaim("uid"), request);
    }

    @PostMapping("/recuperacion/solicitar")
    public MensajeResponse solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        return passwordRecoveryService.solicitarCodigo(request);
    }

    @PostMapping("/recuperacion/validar-codigo")
    public ValidarCodigoResponse validarCodigo(@Valid @RequestBody ValidarCodigoRequest request) {
        return passwordRecoveryService.validarCodigo(request);
    }

    @PostMapping("/recuperacion/restablecer")
    public MensajeResponse restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        return passwordRecoveryService.restablecerPassword(request);
    }
}
