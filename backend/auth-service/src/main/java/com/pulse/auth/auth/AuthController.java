package com.pulse.auth.auth;

import com.pulse.auth.auth.dto.LoginRequest;
import com.pulse.auth.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Login and token issuing")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login (recommended)",
            description = "Authenticates with username/password in the request body and returns a JWT.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    /**
     * The test statement literally asks for a GET login. Credentials in a URL leak
     * into server logs, proxies and browser history, so the POST variant above is
     * the recommended one; this endpoint exists for spec compliance and also
     * accepts credentials via headers to at least keep them out of the URL.
     */
    @GetMapping("/login")
    @Operation(summary = "Login (spec compliance)",
            description = "Same as POST /auth/login but via GET. Accepts username/password as query "
                    + "parameters or as X-Username/X-Password headers. Prefer the POST variant: "
                    + "credentials in URLs end up in logs and browser history.")
    public ResponseEntity<LoginResponse> loginViaGet(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestHeader(name = "X-Username", required = false) String headerUsername,
            @RequestHeader(name = "X-Password", required = false) String headerPassword) {

        String effectiveUsername = headerUsername != null ? headerUsername : username;
        String effectivePassword = headerPassword != null ? headerPassword : password;
        if (effectiveUsername == null || effectiveUsername.isBlank()
                || effectivePassword == null || effectivePassword.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Provide credentials as query params (username, password) or headers (X-Username, X-Password)");
        }
        return ResponseEntity.ok(authService.login(effectiveUsername, effectivePassword));
    }
}
