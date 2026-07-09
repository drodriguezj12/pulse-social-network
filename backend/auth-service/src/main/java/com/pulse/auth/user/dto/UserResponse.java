package com.pulse.auth.user.dto;

import com.pulse.auth.user.UserEntity;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String alias) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                user.getAlias());
    }
}
