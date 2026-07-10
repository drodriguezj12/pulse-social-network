package com.pulse.auth.user;

import com.pulse.auth.auth.dto.LoginResponse;
import com.pulse.auth.security.AuthenticatedUser;
import com.pulse.auth.security.JwtService;
import com.pulse.auth.user.dto.UpdateProfileRequest;
import com.pulse.auth.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Authenticated user profile and avatars")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile",
            description = "Returns first name, last name, birth date and alias of the user owning the JWT.")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        log.info("AUDIT profile_viewed userId={} username={}", principal.id(), principal.username());
        return userService.getProfile(principal.id());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get another user's profile",
            description = "Authenticated users can view profiles, but only /users/me can be edited.")
    public UserResponse profile(@AuthenticationPrincipal AuthenticatedUser principal,
                                @PathVariable UUID userId) {
        log.info("AUDIT profile_viewed viewerId={} username={} targetId={}",
                principal.id(), principal.username(), userId);
        return userService.getProfile(userId);
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's alias",
            description = "Only alias is editable; login identity (username) and real names stay immutable.")
    public LoginResponse updateMe(@AuthenticationPrincipal AuthenticatedUser principal,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = userService.updateProfile(principal.id(), request);
        String token = jwtService.generateToken(user.id(), user.username(), user.alias());
        return new LoginResponse(token, "Bearer", jwtService.expirationMs(), user);
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the authenticated user's profile picture",
            description = "Multipart 'image' part; JPEG/PNG/WebP up to 2MB.")
    public UserResponse uploadAvatar(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @RequestParam("image") MultipartFile image) {
        userService.saveAvatar(principal.id(), image);
        return userService.getProfile(principal.id());
    }

    /**
     * Public on purpose: avatars are rendered with plain img tags (which cannot
     * attach Authorization headers) in every user's feed. Only the image bytes
     * are exposed; profile data stays behind the JWT.
     */
    @GetMapping("/{userId}/avatar")
    @Operation(summary = "Get a user's profile picture (public)",
            description = "Returns the image bytes, or 404 when the user has no picture.")
    public ResponseEntity<byte[]> avatar(@PathVariable UUID userId) {
        UserAvatarEntity avatar = userService.getAvatar(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .body(avatar.getImage());
    }
}
