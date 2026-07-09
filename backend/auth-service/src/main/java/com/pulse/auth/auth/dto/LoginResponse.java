package com.pulse.auth.auth.dto;

import com.pulse.auth.user.dto.UserResponse;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user) {
}
