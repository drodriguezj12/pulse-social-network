package com.pulse.auth.common;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

/** Consistent error payload returned by every endpoint of this service. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
