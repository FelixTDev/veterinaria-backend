package com.veterinaria.backend.auth.service;

import java.util.regex.Pattern;

import com.veterinaria.backend.auth.exception.PasswordPolicyException;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])\\S{8,}$");

    public void validate(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new PasswordPolicyException(
                    "La password debe tener minimo 8 caracteres, mayuscula, minuscula, numero, caracter especial y no contener espacios.");
        }
    }
}
