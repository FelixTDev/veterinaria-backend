package com.veterinaria.backend.shared.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String code,
        Map<String, String> fieldErrors) {
}
