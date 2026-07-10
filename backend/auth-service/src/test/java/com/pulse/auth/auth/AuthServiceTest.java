package com.pulse.auth.auth;

import com.pulse.auth.auth.dto.LoginResponse;
import com.pulse.auth.common.InvalidCredentialsException;
import com.pulse.auth.security.JwtService;
import com.pulse.auth.user.UserAvatarRepository;
import com.pulse.auth.user.UserEntity;
import com.pulse.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAvatarRepository avatarRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    private AuthService authService;
    private UserEntity mariana;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, avatarRepository, passwordEncoder, jwtService);
        mariana = new UserEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "mariana",
                passwordEncoder.encode("Pulse2026!"),
                "Mariana", "López", LocalDate.of(1995, 4, 12), "marilo");
    }

    @Test
    @DisplayName("valid credentials return a token that carries the user identity")
    void loginHappyPath() {
        when(userRepository.findByUsername("mariana")).thenReturn(Optional.of(mariana));

        LoginResponse response = authService.login("mariana", "Pulse2026!");

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3_600_000L);
        assertThat(response.user().username()).isEqualTo("mariana");
        assertThat(response.user().alias()).isEqualTo("marilo");
        assertThat(jwtService.parse(response.token()).id()).isEqualTo(mariana.getId());
    }

    @Test
    @DisplayName("wrong password is rejected with 401 semantics")
    void wrongPasswordRejected() {
        when(userRepository.findByUsername("mariana")).thenReturn(Optional.of(mariana));

        assertThatThrownBy(() -> authService.login("mariana", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("unknown user is rejected with the same message as a wrong password")
    void unknownUserRejected() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
