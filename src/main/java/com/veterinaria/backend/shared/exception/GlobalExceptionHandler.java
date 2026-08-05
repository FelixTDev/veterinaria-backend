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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
