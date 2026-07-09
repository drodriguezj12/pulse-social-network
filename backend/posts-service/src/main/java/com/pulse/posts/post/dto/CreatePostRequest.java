package com.pulse.posts.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "message is required")
        @Size(max = 500, message = "message must be at most 500 characters")
        String message) {
}
