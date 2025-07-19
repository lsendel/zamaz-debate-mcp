package com.zamaz.mcp.configserver.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServerHealthIndicatorTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private org.springframework.cloud.config.environment.Environment mockEnvironment;

    private ConfigServerHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new ConfigServerHealthIndicator(environmentRepository);
    }

    @Test
    void testHealthyConfigServer() {
        // Given
        when(environmentRepository.findOne(anyString(), anyString(), anyString()))
                .thenReturn(mockEnvironment);
        when(mockEnvironment.getName()).thenReturn("test-app");

        // When
        Health health = healthIndicator.health();

        // Then
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("repository"));
        assertEquals("available", health.getDetails().get("repository"));
    }

    @Test
    void testUnhealthyConfigServer() {
        // Given
        when(environmentRepository.findOne(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Repository unavailable"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
        assertEquals("Repository unavailable", health.getDetails().get("error"));
    }

    @Test
    void testHealthWithRepositoryDetails() {
        // Given
        when(environmentRepository.findOne(anyString(), anyString(), anyString()))
                .thenReturn(mockEnvironment);
        when(mockEnvironment.getName()).thenReturn("mcp-organization");
        when(mockEnvironment.getProfiles()).thenReturn(new String[]{"development"});
        when(mockEnvironment.getLabel()).thenReturn("main");

        // When
        Health health = healthIndicator.health();

        // Then
        assertEquals(Status.UP, health.getStatus());
        assertEquals("available", health.getDetails().get("repository"));
        assertTrue(health.getDetails().containsKey("lastCheck"));
    }
}