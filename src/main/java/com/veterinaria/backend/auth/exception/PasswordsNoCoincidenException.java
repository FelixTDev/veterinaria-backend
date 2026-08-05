package com.veterinaria.backend.auth.exception;

public class PasswordsNoCoincidenException extends AuthException {

    public PasswordsNoCoincidenException(String message) {
        super(message);
    }
}
