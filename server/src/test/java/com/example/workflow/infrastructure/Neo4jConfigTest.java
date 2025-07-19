package com.example.workflow.infrastructure;

import com.example.workflow.infrastructure.config.Neo4jConfig;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for Neo4j configuration and connectivity
 */
@SpringBootTest
@Testcontainers
class Neo4jConfigTest {
    
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.13")
            .withAdminPassword("testpassword")
            .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
    }
    
    @Autowired
    private Neo4jConfig neo4jConfig;
    
    @Autowired
    private Driver driver;
    
    @Test
    void shouldConnectToNeo4j() {
        // When
        try (Session session = driver.session()) {
            var result = session.run("RETURN 'Hello Neo4j' as message");
            String message = result.single().get("message").asString();
            
            // Then
            assertThat(message).isEqualTo("Hello Neo4j");
        }
    }
    
    @Test
    void shouldHaveConstraintsCreated() {
        // When
        try (Session session = driver.session()) {
            var result = session.run("SHOW CONSTRAINTS");
            
            // Then
            assertThat(result.list()).isNotEmpty();
        }
    }
    
    @Test
    void shouldHaveIndexesCreated() {
        // When
        try (Session session = driver.session()) {
            var result = session.run("SHOW INDEXES");
            
            // Then
            assertThat(result.list()).isNotEmpty();
        }
    }
    
    @Test
    void shouldHaveCorrectBatchSize() {
        // Then
        assertThat(neo4jConfig.getBatchSize()).isGreaterThan(0);
    }
}