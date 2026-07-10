package com.pulse.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "firstName is required")
        @Size(max = 80, message = "firstName must be at most 80 characters")
        String firstName,
        @NotBlank(message = "lastName is required")
        @Size(max = 80, message = "lastName must be at most 80 characters")
        String lastName) {
}
