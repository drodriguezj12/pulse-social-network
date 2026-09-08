package com.pulse.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank(message = "alias is required")
        @Pattern(regexp = "^[a-zA-Z0-9._]{3,50}$",
                message = "alias must be 3 to 50 characters, using only letters, digits, dots or underscores")
        String alias) {
}
