package com.pulse.auth.user;

import com.pulse.auth.security.AuthenticatedUser;
import com.pulse.auth.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Authenticated user profile")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile",
            description = "Returns first name, last name, birth date and alias of the user owning the JWT.")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        log.info("AUDIT profile_viewed userId={} username={}", principal.id(), principal.username());
        return userService.getProfile(principal.id());
    }
}
