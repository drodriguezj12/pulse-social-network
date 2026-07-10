package com.pulse.auth.auth;

import com.pulse.auth.auth.dto.LoginResponse;
import com.pulse.auth.common.InvalidCredentialsException;
import com.pulse.auth.security.JwtService;
import com.pulse.auth.user.UserAvatarRepository;
import com.pulse.auth.user.UserEntity;
import com.pulse.auth.user.UserRepository;
import com.pulse.auth.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserAvatarRepository avatarRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserAvatarRepository avatarRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("AUDIT login_failed username={} reason=unknown_user", username);
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("AUDIT login_failed username={} reason=bad_password", username);
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getAlias());
        log.info("AUDIT login_success userId={} username={}", user.getId(), user.getUsername());
        return new LoginResponse(token, "Bearer", jwtService.expirationMs(),
                UserResponse.from(user, avatarRepository.existsById(user.getId())));
    }
}
