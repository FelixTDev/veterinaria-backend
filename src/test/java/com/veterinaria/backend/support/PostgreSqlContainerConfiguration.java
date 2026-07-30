package com.veterinaria.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class PostgreSqlContainerConfiguration {

    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("veterinaria_test_db")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("sql/script_inicial_veterinaria_postgresql.sql");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("app.security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
        registry.add("app.security.jwt.expiration-minutes", () -> "60");
        registry.add("app.security.jwt.recovery-expiration-minutes", () -> "10");
        registry.add("app.auth.mail.from", () -> "test@veterinaria.local");
    }
}
