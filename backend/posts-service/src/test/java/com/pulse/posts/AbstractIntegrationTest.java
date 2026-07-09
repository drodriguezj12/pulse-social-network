package com.pulse.posts;

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

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("socialdb")
                    .withUsername("pulse")
                    .withPassword("pulse");

    static {
        POSTGRES.start();
    }

    /** Same URL tweak the app uses: {call ...} must emit CALL for procedures. */
    public static String jdbcUrlWithCallMode() {
        String url = POSTGRES.getJdbcUrl();
        return url + (url.contains("?") ? "&" : "?") + "escapeSyntaxCallMode=callIfNoReturn";
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractIntegrationTest::jdbcUrlWithCallMode);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
