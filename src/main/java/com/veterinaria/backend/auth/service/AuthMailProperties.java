package com.veterinaria.backend.auth.service;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.mail")
public class AuthMailProperties {

    private String from;

    @Min(1)
    private int recoveryCodeExpirationMinutes;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public int getRecoveryCodeExpirationMinutes() {
        return recoveryCodeExpirationMinutes;
    }

    public void setRecoveryCodeExpirationMinutes(int recoveryCodeExpirationMinutes) {
        this.recoveryCodeExpirationMinutes = recoveryCodeExpirationMinutes;
    }
}
