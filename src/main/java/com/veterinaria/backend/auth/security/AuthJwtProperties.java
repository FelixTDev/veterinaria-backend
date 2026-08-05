package com.veterinaria.backend.auth.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public class AuthJwtProperties {

    @NotBlank
    @Size(min = 32)
    private String secret;

    @Min(1)
    private long expirationMinutes;

    @Min(1)
    private long recoveryExpirationMinutes;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getRecoveryExpirationMinutes() {
        return recoveryExpirationMinutes;
    }

    public void setRecoveryExpirationMinutes(long recoveryExpirationMinutes) {
        this.recoveryExpirationMinutes = recoveryExpirationMinutes;
    }
}
