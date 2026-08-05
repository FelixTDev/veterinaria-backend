package com.veterinaria.backend.shared.exception;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import com.veterinaria.backend.auth.exception.AuthException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionExpiradoException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionInvalidoException;
import com.veterinaria.backend.auth.exception.CodigoRecuperacionUsadoException;
import com.veterinaria.backend.auth.exception.CorreoEnvioException;
import com.veterinaria.backend.auth.exception.CredencialesInvalidasException;
import com.veterinaria.backend.auth.exception.CuentaBloqueadaException;
import com.veterinaria.backend.auth.exception.PasswordActualIncorrectaException;
import com.veterinaria.backend.auth.exception.PasswordPolicyException;
import com.veterinaria.backend.auth.exception.PasswordsNoCoincidenException;
import com.veterinaria.backend.auth.exception.RecoveryTokenInvalidoException;
import com.veterinaria.backend.auth.exception.UsuarioInactivoException;
import com.veterinaria.backend.usuario.exception.CorreoDuplicadoException;
import com.veterinaria.backend.usuario.exception.OperacionAdministradorException;
import com.veterinaria.backend.usuario.exception.RolInvalidoException;
import com.veterinaria.backend.usuario.exception.UsuarioNoEncontradoException;
import com.veterinaria.backend.cliente.exception.ClienteNoEncontradoException;
import com.veterinaria.backend.cliente.exception.DocumentoClienteDuplicadoException;
import com.veterinaria.backend.cliente.exception.PageSizeClienteInvalidoException;
import com.veterinaria.backend.mascota.exception.ClienteMascotaInactivoException;
import com.veterinaria.backend.mascota.exception.MascotaNoEncontradaException;
import com.veterinaria.backend.mascota.exception.PageSizeMascotaInvalidoException;
import com.veterinaria.backend.servicio.exception.PrecioServicioInvalidoException;
import com.veterinaria.backend.servicio.exception.ServicioDuplicadoException;
import com.veterinaria.backend.servicio.exception.ServicioNoEncontradoException;
import com.veterinaria.backend.horario.exception.IndisponibilidadNoEncontradaException;
import com.veterinaria.backend.horario.exception.IndisponibilidadSolapadaException;
import com.veterinaria.backend.horario.exception.HorarioNoEncontradoException;
import com.veterinaria.backend.horario.exception.HorarioSolapadoException;
import com.veterinaria.backend.horario.exception.RangoHorarioInvalidoException;
import com.veterinaria.backend.horario.exception.RangoIndisponibilidadInvalidoException;
import com.veterinaria.backend.horario.exception.TrabajadorNoEncontradoException;
import com.veterinaria.backend.horario.exception.TrabajadorNoProgramableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            CredencialesInvalidasException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveUser(
            UsuarioInactivoException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ApiErrorResponse> handleBlockedAccount(
            CuentaBloqueadaException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.LOCKED, exception.getMessage(), request);
    }

    @ExceptionHandler({
            PasswordActualIncorrectaException.class,
            PasswordsNoCoincidenException.class,
            PasswordPolicyException.class,
            CodigoRecuperacionInvalidoException.class,
            CodigoRecuperacionExpiradoException.class,
            CodigoRecuperacionUsadoException.class,
            RecoveryTokenInvalidoException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            AuthException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(CorreoEnvioException.class)
    public ResponseEntity<ApiErrorResponse> handleMailError(
            CorreoEnvioException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioNotFound(
            UsuarioNoEncontradoException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ClienteNoEncontradoException.class,
            MascotaNoEncontradaException.class,
            ServicioNoEncontradoException.class,
            TrabajadorNoEncontradoException.class,
            HorarioNoEncontradoException.class,
            IndisponibilidadNoEncontradaException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDomainNotFound(
            RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({
            DocumentoClienteDuplicadoException.class,
            ClienteMascotaInactivoException.class,
            ServicioDuplicadoException.class,
            PrecioServicioInvalidoException.class,
            TrabajadorNoProgramableException.class,
            HorarioSolapadoException.class,
            IndisponibilidadSolapadaException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDomainConflict(
            RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            PageSizeClienteInvalidoException.class,
            PageSizeMascotaInvalidoException.class,
            RangoHorarioInvalidoException.class,
            RangoIndisponibilidadInvalidoException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDomainBadRequest(
            RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(RolInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleRolInvalido(
            RolInvalidoException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({
            CorreoDuplicadoException.class,
            OperacionAdministradorException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUsuarioConflict(
            RuntimeException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            RuntimeException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno.", request);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
