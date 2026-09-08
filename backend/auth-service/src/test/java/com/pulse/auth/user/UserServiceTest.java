package com.pulse.auth.user;

import com.pulse.auth.common.NotFoundException;
import com.pulse.auth.user.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAvatarRepository avatarRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("profile of an existing user is returned with all fields")
    void profileFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(new UserEntity(
                id, "daniela", "hash", "Daniela", "Mora", LocalDate.of(1996, 9, 30), "danim")));
        when(avatarRepository.existsById(id)).thenReturn(true);

        UserResponse profile = userService.getProfile(id);

        assertThat(profile.firstName()).isEqualTo("Daniela");
        assertThat(profile.lastName()).isEqualTo("Mora");
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(1996, 9, 30));
        assertThat(profile.alias()).isEqualTo("danim");
        assertThat(profile.hasAvatar()).isTrue();
    }

    @Test
    @DisplayName("updateProfile changes alias only")
    void updateProfileChangesAlias() {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity(
                id, "daniela", "hash", "Daniela", "Mora", LocalDate.of(1996, 9, 30), "danim");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByAliasIgnoreCaseAndIdNot("dani_mora", id)).thenReturn(false);
        when(avatarRepository.existsById(id)).thenReturn(false);

        UserResponse updated = userService.updateProfile(id,
                new com.pulse.auth.user.dto.UpdateProfileRequest("  dani_mora "));

        assertThat(updated.firstName()).isEqualTo("Daniela");
        assertThat(updated.lastName()).isEqualTo("Mora");
        assertThat(updated.username()).isEqualTo("daniela");
        assertThat(updated.alias()).isEqualTo("dani_mora");
    }

    @Test
    @DisplayName("an alias another user already holds is rejected with 409")
    void updateProfileRejectsTakenAlias() {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity(
                id, "daniela", "hash", "Daniela", "Mora", LocalDate.of(1996, 9, 30), "danim");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByAliasIgnoreCaseAndIdNot("marilo", id)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(id,
                new com.pulse.auth.user.dto.UpdateProfileRequest("marilo")))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("already taken");

        assertThat(user.getAlias()).isEqualTo("danim");   // unchanged
    }

    @Test
    @DisplayName("avatar with a non-image content type is rejected")
    void avatarWrongTypeRejected() {
        var file = new org.springframework.mock.web.MockMultipartFile(
                "image", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> userService.saveAvatar(UUID.randomUUID(), file))
                .isInstanceOf(com.pulse.auth.common.InvalidImageException.class);
    }

    @Test
    @DisplayName("missing user raises NotFoundException (mapped to 404)")
    void profileNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
