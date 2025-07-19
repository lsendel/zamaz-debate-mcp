package com.zamaz.mcp.configserver.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConfigServerHealthIndicator implements HealthIndicator {

    private final EnvironmentRepository environmentRepository;

    public ConfigServerHealthIndicator(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    public Health health() {
        try {
            // Test repository access
            environmentRepository.findOne("health-check", "default", "main");
            
            return Health.up()
                    .withDetail("repository", "available")
                    .withDetail("lastCheck", LocalDateTime.now().toString())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("repository", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}