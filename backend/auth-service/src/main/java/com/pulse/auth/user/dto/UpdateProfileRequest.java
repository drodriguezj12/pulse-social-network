package com.pulse.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "alias is required")
        @Size(max = 50, message = "alias must be at most 50 characters")
        String alias) {
}
