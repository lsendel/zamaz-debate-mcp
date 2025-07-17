package com.zamaz.mcp.testing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Base class for integration tests with Testcontainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected RequestSpecification requestSpec;
    
    // PostgreSQL container
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("mcp_test")
            .withUsername("test")
            .withPassword("test")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(2));
    
    // Redis container
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(1));
    
    // Jaeger container for tracing
    @Container
    static GenericContainer<?> jaeger = new GenericContainer<>(
            DockerImageName.parse("jaegertracing/all-in-one:latest"))
            .withExposedPorts(16686, 14250)
            .withEnv("COLLECTOR_ZIPKIN_HOST_PORT", ":9411")
            .waitingFor(Wait.forHttp("/").forPort(16686))
            .withStartupTimeout(Duration.ofMinutes(2));
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Redis
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        // Jaeger
        registry.add("tracing.jaeger.endpoint", 
            () -> String.format("http://%s:%d", jaeger.getHost(), jaeger.getMappedPort(14250)));
        
        // Disable Chaos Monkey for integration tests
        registry.add("chaos.monkey.enabled", () -> "false");
    }
    
    @BeforeAll
    static void beforeAll() {
        // Wait for containers to be ready
        postgres.start();
        redis.start();
        jaeger.start();
    }
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        
        requestSpec = new RequestSpecBuilder()
            .setPort(port)
            .setContentType("application/json")
            .setAccept("application/json")
            .build();
    }
    
    /**
     * Create an authenticated request specification
     */
    protected RequestSpecification authenticatedRequest(String token, String organizationId) {
        return new RequestSpecBuilder()
            .addRequestSpecification(requestSpec)
            .addHeader("Authorization", "Bearer " + token)
            .addHeader("X-Organization-ID", organizationId)
            .build();
    }
    
    /**
     * Helper method to create test data
     */
    protected Map<String, Object> createTestDebate() {
        return Map.of(
            "title", "Test Debate",
            "topic", "Test Topic",
            "description", "Test Description",
            "maxRounds", 5,
            "format", "standard",
            "settings", Map.of(
                "turnTimeoutSeconds", 300,
                "allowAudience", true
            )
        );
    }
    
    /**
     * Helper method to authenticate and get token
     */
    protected String authenticate(String username, String password, String organizationId) {
        Map<String, String> credentials = Map.of(
            "username", username,
            "password", password,
            "organizationId", organizationId
        );
        
        return RestAssured
            .given()
                .spec(requestSpec)
                .body(credentials)
            .when()
                .post("/api/v1/auth/login")
            .then()
                .statusCode(200)
                .extract()
                .path("token");
    }
}