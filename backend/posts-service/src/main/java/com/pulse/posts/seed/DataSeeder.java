package com.pulse.posts.seed;

import com.pulse.posts.post.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Seeds one post per demo user (spec requirement). Author UUIDs and aliases
 * match auth-service's seeder by convention — the two seeders stay
 * independent so neither service calls the other at startup.
 * Fixed post UUIDs keep this consistent with the standalone db/seed.sql.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PostRepository postRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(PostRepository postRepository, JdbcTemplate jdbcTemplate) {
        this.postRepository = postRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (postRepository.count() > 0) {
            log.info("Seed skipped: posts already present");
            return;
        }

        insertPost("10000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000001",
                "marilo", "Just joined Pulse — excited to share things here! 🚀");
        insertPost("10000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000002",
                "cgomez", "Hot take: PostgreSQL stored procedures are underrated.");
        insertPost("10000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000003",
                "valen", "Weekend plan: coffee, code and a good playlist ☕");
        insertPost("10000000-0000-0000-0000-000000000004", "00000000-0000-0000-0000-000000000004",
                "atorres", "Shipping a side project tonight. Wish me luck!");
        insertPost("10000000-0000-0000-0000-000000000005", "00000000-0000-0000-0000-000000000005",
                "danim", "Watching likes update in real time is oddly satisfying ✨");

        log.info("AUDIT seed_completed posts=5 (one per demo user)");
    }

    private void insertPost(String postId, String authorId, String alias, String message) {
        // Plain SQL keeps the fixed UUIDs and lets published_at use the DB default,
        // mirroring exactly what db/seed.sql produces.
        jdbcTemplate.update(
                "INSERT INTO posts.posts (id, author_id, author_alias, message) VALUES (?, ?, ?, ?)",
                UUID.fromString(postId), UUID.fromString(authorId), alias, message);
    }
}
