package com.pulse.auth.seed;

import com.pulse.auth.user.UserAvatarEntity;
import com.pulse.auth.user.UserAvatarRepository;
import com.pulse.auth.user.UserEntity;
import com.pulse.auth.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Seeds five demo users on startup when the table is empty. The UUIDs are
 * fixed by convention and shared with posts-service's seeder so that seeded
 * posts reference real users without coupling the services at runtime.
 * The same data ships as a standalone script in db/seed.sql.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String DEMO_PASSWORD = "Pulse2026!";

    private final UserRepository userRepository;
    private final UserAvatarRepository avatarRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, UserAvatarRepository avatarRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped: users already present");
            return;
        }

        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        List<UserEntity> users = List.of(
                user("00000000-0000-0000-0000-000000000001", "mariana", hash,
                        "Mariana", "López", LocalDate.of(1995, 4, 12), "marilo"),
                user("00000000-0000-0000-0000-000000000002", "carlos", hash,
                        "Carlos", "Gómez", LocalDate.of(1992, 11, 3), "cgomez"),
                user("00000000-0000-0000-0000-000000000003", "valentina", hash,
                        "Valentina", "Ruiz", LocalDate.of(1998, 7, 21), "valen"),
                user("00000000-0000-0000-0000-000000000004", "andres", hash,
                        "Andrés", "Torres", LocalDate.of(1990, 2, 14), "atorres"),
                user("00000000-0000-0000-0000-000000000005", "daniela", hash,
                        "Daniela", "Mora", LocalDate.of(1996, 9, 30), "danim"));

        userRepository.saveAll(users);

        // Two of them get a picture, so the feed shows both real avatars and the
        // generated discs the other users fall back to.
        seedAvatar("00000000-0000-0000-0000-000000000001", "seed/avatar-marilo.jpg");
        seedAvatar("00000000-0000-0000-0000-000000000002", "seed/avatar-cgomez.jpg");

        log.info("AUDIT seed_completed users={} (password documented in README)", users.size());
    }

    private UserEntity user(String id, String username, String hash,
                            String firstName, String lastName, LocalDate birthDate, String alias) {
        return new UserEntity(UUID.fromString(id), username, hash, firstName, lastName, birthDate, alias);
    }

    private void seedAvatar(String userId, String resource) {
        try {
            byte[] image = new ClassPathResource(resource).getContentAsByteArray();
            avatarRepository.save(new UserAvatarEntity(UUID.fromString(userId), "image/jpeg", image));
        } catch (IOException e) {
            log.warn("Demo avatar {} not seeded: {}", resource, e.getMessage());
        }
    }
}
