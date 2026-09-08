package com.pulse.auth;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the full Spring context against a real PostgreSQL 16 (Testcontainers).
 * The container is a singleton shared by every IT class to keep the suite fast.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("socialdb")
                    .withUsername("pulse")
                    .withPassword("pulse");

    static {
        POSTGRES.start();
    }

    /** The application requires JWT_SECRET; tests supply their own. */
    public static final String TEST_JWT_SECRET = "integration-test-secret-0123456789-0123456789";

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }
}
