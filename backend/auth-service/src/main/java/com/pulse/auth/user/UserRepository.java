package com.pulse.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    /** Aliases are unique case insensitively; see V3__unique_alias.sql. */
    boolean existsByAliasIgnoreCaseAndIdNot(String alias, UUID id);
}
