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

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("profile of an existing user is returned with all fields")
    void profileFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(new UserEntity(
                id, "daniela", "hash", "Daniela", "Mora", LocalDate.of(1996, 9, 30), "danim")));

        UserResponse profile = userService.getProfile(id);

        assertThat(profile.firstName()).isEqualTo("Daniela");
        assertThat(profile.lastName()).isEqualTo("Mora");
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(1996, 9, 30));
        assertThat(profile.alias()).isEqualTo("danim");
    }

    @Test
    @DisplayName("missing user raises NotFoundException (mapped to 404)")
    void profileNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
