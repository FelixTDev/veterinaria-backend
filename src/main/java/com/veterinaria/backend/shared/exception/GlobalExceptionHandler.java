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
import com.veterinaria.backend.cita.exception.CitaBadRequestException;
import com.veterinaria.backend.cita.exception.CitaConflictException;
import com.veterinaria.backend.cita.exception.CitaNoEncontradaException;
import com.veterinaria.backend.atencionmedica.exception.AtencionMedicaConflictException;
import com.veterinaria.backend.atencionmedica.exception.AtencionMedicaNoEncontradaException;
import com.veterinaria.backend.vacuna.exception.VacunaDuplicadaException;
import com.veterinaria.backend.vacuna.exception.VacunaInactivaException;
import com.veterinaria.backend.vacuna.exception.VacunaNoEncontradaException;
import com.veterinaria.backend.vacuna.exception.VacunacionNoPermitidaException;
import com.veterinaria.backend.peluqueria.exception.AtencionPeluqueriaNoEncontradaException;
import com.veterinaria.backend.peluqueria.exception.EvidenciaInvalidaException;
import com.veterinaria.backend.peluqueria.exception.PeluqueriaConflictException;
import com.veterinaria.backend.pago.exception.PagoConflictException;
import com.veterinaria.backend.pago.exception.PagoNoEncontradoException;
import com.veterinaria.backend.comprobante.exception.ComprobanteConflictException;
import com.veterinaria.backend.comprobante.exception.ComprobanteNoEncontradoException;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleRequestBinding(
            Exception exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Los parametros de la solicitud no son validos.", request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            CredencialesInvalidasException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage(), request);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveUser(
            UsuarioInactivoException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), request);
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ApiErrorResponse> handleBlockedAccount(
            CuentaBloqueadaException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.LOCKED, "ACCOUNT_LOCKED", exception.getMessage(), request);
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
        return buildResponse(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", exception.getMessage(), request);
    }

    @ExceptionHandler(CorreoEnvioException.class)
    public ResponseEntity<ApiErrorResponse> handleMailError(
            CorreoEnvioException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", exception.getMessage(), request);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioNotFound(
            UsuarioNoEncontradoException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({
            ClienteNoEncontradoException.class,
            MascotaNoEncontradaException.class,
            ServicioNoEncontradoException.class,
            CitaNoEncontradaException.class,
            TrabajadorNoEncontradoException.class,
            HorarioNoEncontradoException.class,
            IndisponibilidadNoEncontradaException.class
            ,AtencionMedicaNoEncontradaException.class,
            VacunaNoEncontradaException.class
            ,AtencionPeluqueriaNoEncontradaException.class,
            PagoNoEncontradoException.class,
            ComprobanteNoEncontradoException.class
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
            CitaConflictException.class,
            TrabajadorNoProgramableException.class,
            HorarioSolapadoException.class,
            IndisponibilidadSolapadaException.class
            ,AtencionMedicaConflictException.class,
            VacunaDuplicadaException.class,
            VacunaInactivaException.class,
            VacunacionNoPermitidaException.class
            ,PeluqueriaConflictException.class,
            PagoConflictException.class,
            ComprobanteConflictException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDomainConflict(
            RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler({
            PageSizeClienteInvalidoException.class,
            PageSizeMascotaInvalidoException.class,
            RangoHorarioInvalidoException.class,
            RangoIndisponibilidadInvalidoException.class,
            CitaBadRequestException.class,
            IllegalArgumentException.class,
            EvidenciaInvalidaException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDomainBadRequest(
            RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), request);
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
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acceso denegado.", request);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationCredentialsNotFound(
            AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Token invalido.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrio un error interno.", request);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        return buildResponse(status, defaultCode(status), message, request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message,
            HttpServletRequest request) {
        return buildResponse(status, code, message, request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message,
            HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(), code, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String defaultCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "VALIDATION_ERROR";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "RESOURCE_NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            default -> "HTTP_ERROR";
        };
    }
}
