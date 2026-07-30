package com.veterinaria.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseConnectionIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldOpenConnectionWhenDatabaseCredentialsAreConfigured() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
    }
}
