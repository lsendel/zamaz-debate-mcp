package com.zamaz.mcp.github.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for TestContainers
 * Provides shared container instances for E2E tests
 */
@TestConfiguration
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.datasource.hikari.maximum-pool-size=20",
    "spring.redis.timeout=10s",
    "logging.level.com.zamaz.mcp.github=DEBUG",
    "github.webhook.secret=test-secret-key-for-testing",
    "notifications.enabled=true"
})
public class TestContainerConfiguration {

    private static final String POSTGRES_IMAGE = "postgres:15";
    private static final String REDIS_IMAGE = "redis:7-alpine";

    /**
     * PostgreSQL container for testing
     */
    @Bean
    @Primary
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName("github_integration_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("init-test-db.sql");
        
        container.start();
        return container;
    }

    /**
     * Redis container for testing
     */
    @Bean
    @Primary
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(6379)
                .withReuse(true);
        
        container.start();
        return container;
    }

    /**
     * WireMock container for GitHub API mocking
     */
    @Bean
    @Primary
    public GenericContainer<?> wireMockContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:latest"))
                .withExposedPorts(8080)
                .withCommand("--port", "8080", "--verbose")
                .withReuse(true);
        
        container.start();
        return container;
    }
}