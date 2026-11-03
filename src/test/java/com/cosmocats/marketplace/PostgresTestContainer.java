package com.cosmocats.marketplace;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One PostgreSQL container for all integration tests of a test JVM. It is started once,
 * Liquibase creates the schema when the first Spring context starts, and Testcontainers
 * removes the container when the JVM exits.
 */
public final class PostgresTestContainer {

    // Same major version as scripts/docker/compose.yaml
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private PostgresTestContainer() {
    }

    public static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
