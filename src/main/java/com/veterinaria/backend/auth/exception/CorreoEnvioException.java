package com.veterinaria.backend.auth.exception;

public class CorreoEnvioException extends AuthException {

    public CorreoEnvioException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public CorreoEnvioException(String message) {
        super(message);
    }
}
