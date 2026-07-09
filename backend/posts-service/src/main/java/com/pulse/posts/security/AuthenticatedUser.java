package com.pulse.posts.security;

import java.util.UUID;

/** Principal stored in the SecurityContext once a JWT has been validated. */
public record AuthenticatedUser(UUID id, String username, String alias) {
}
