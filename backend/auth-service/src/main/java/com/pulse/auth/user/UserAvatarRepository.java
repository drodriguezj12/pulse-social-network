package com.pulse.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAvatarRepository extends JpaRepository<UserAvatarEntity, UUID> {
}
