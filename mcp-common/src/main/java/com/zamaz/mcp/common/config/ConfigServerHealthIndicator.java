package com.zamaz.mcp.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Health indicator for Spring Cloud Config Server.
 * Monitors the availability and health of the Config Server.
 */
@Component
@ConditionalOnProperty(name = "spring.cloud.config.enabled", havingValue = "true", matchIfMissing = true)
public class ConfigServerHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigServerHealthIndicator.class);

    @Value("${spring.cloud.config.uri:http://localhost:8888}")
    private String configServerUri;

    @Value("${spring.cloud.config.username:}")
    private String username;

    @Value("${spring.cloud.config.password:}")
    private String password;

    @Value("${spring.cloud.config.health-check.timeout:5000}")
    private int healthCheckTimeout;

    @Value("${spring.cloud.config.health-check.enabled:true}")
    private boolean healthCheckEnabled;

    private final RestTemplate restTemplate;
    private LocalDateTime lastSuccessfulCheck;
    private LocalDateTime lastFailedCheck;
    private String lastErrorMessage;
    private long totalChecks = 0;
    private long successfulChecks = 0;

    public ConfigServerHealthIndicator() {
        this.restTemplate = createRestTemplate();
    }

    @Override
    public Health health() {
        if (!healthCheckEnabled) {
            return Health.up()
                .withDetail("status", "Health check disabled")
                .build();
        }

        totalChecks++;
        Map<String, Object> details = new ConcurrentHashMap<>();
        
        try {
            // Check Config Server health endpoint
            String healthUrl = configServerUri + "/actuator/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                lastSuccessfulCheck = LocalDateTime.now();
                successfulChecks++;
                
                return Health.up()
                    .withDetail("configServer", configServerUri)
                    .withDetail("status", "Available")
                    .withDetail("lastSuccessfulCheck", lastSuccessfulCheck)
                    .withDetail("uptime", calculateUptime())
                    .withDetail("successRate", calculateSuccessRate())
                    .withDetails(extractHealthDetails(response.getBody()))
                    .build();
            } else {
                lastFailedCheck = LocalDateTime.now();
                lastErrorMessage = "Unexpected status: " + response.getStatusCode();
                
                return Health.down()
                    .withDetail("configServer", configServerUri)
                    .withDetail("status", "Unhealthy")
                    .withDetail("statusCode", response.getStatusCode())
                    .withDetail("lastFailedCheck", lastFailedCheck)
                    .withDetail("errorMessage", lastErrorMessage)
                    .build();
            }
        } catch (Exception e) {
            lastFailedCheck = LocalDateTime.now();
            lastErrorMessage = e.getMessage();
            logger.error("Config Server health check failed", e);
            
            return Health.down()
                .withDetail("configServer", configServerUri)
                .withDetail("status", "Unavailable")
                .withDetail("lastFailedCheck", lastFailedCheck)
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("errorMessage", lastErrorMessage)
                .withDetail("successRate", calculateSuccessRate())
                .build();
        }
    }

    /**
     * Creates a RestTemplate with timeout configuration.
     */
    private RestTemplate createRestTemplate() {
        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory =
            new org.springframework.http.client.HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(healthCheckTimeout);
        factory.setReadTimeout(healthCheckTimeout);
        
        RestTemplate template = new RestTemplate(factory);
        
        // Add basic authentication if configured
        if (!username.isEmpty() && !password.isEmpty()) {
            template.getInterceptors().add(new org.springframework.http.client.support.BasicAuthenticationInterceptor(username, password));
        }
        
        return template;
    }

    /**
     * Extracts relevant health details from the Config Server response.
     */
    private Map<String, Object> extractHealthDetails(Map<String, Object> healthResponse) {
        Map<String, Object> details = new ConcurrentHashMap<>();
        
        if (healthResponse != null) {
            // Extract status
            Object status = healthResponse.get("status");
            if (status != null) {
                details.put("configServerStatus", status);
            }
            
            // Extract components health
            Object components = healthResponse.get("components");
            if (components instanceof Map) {
                Map<String, Object> componentMap = (Map<String, Object>) components;
                if (componentMap.containsKey("git")) {
                    details.put("gitRepository", "Connected");
                }
                if (componentMap.containsKey("vault")) {
                    details.put("vault", "Connected");
                }
            }
        }
        
        return details;
    }

    /**
     * Calculates the uptime percentage based on successful checks.
     */
    private String calculateUptime() {
        if (lastSuccessfulCheck == null) {
            return "Never connected";
        }
        
        if (lastFailedCheck == null) {
            return "100%";
        }
        
        Duration uptime = Duration.between(lastFailedCheck, LocalDateTime.now());
        Duration totalTime = Duration.between(
            lastSuccessfulCheck.isBefore(lastFailedCheck) ? lastSuccessfulCheck : lastFailedCheck,
            LocalDateTime.now()
        );
        
        if (totalTime.isZero()) {
            return "100%";
        }
        
        double uptimePercentage = (double) uptime.toMillis() / totalTime.toMillis() * 100;
        return String.format("%.2f%%", uptimePercentage);
    }

    /**
     * Calculates the success rate of health checks.
     */
    private String calculateSuccessRate() {
        if (totalChecks == 0) {
            return "N/A";
        }
        
        double successRate = (double) successfulChecks / totalChecks * 100;
        return String.format("%.2f%% (%d/%d)", successRate, successfulChecks, totalChecks);
    }

    /**
     * Gets the last successful check time.
     */
    public LocalDateTime getLastSuccessfulCheck() {
        return lastSuccessfulCheck;
    }

    /**
     * Gets the last failed check time.
     */
    public LocalDateTime getLastFailedCheck() {
        return lastFailedCheck;
    }

    /**
     * Gets the last error message.
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Resets health check statistics.
     */
    public void resetStatistics() {
        totalChecks = 0;
        successfulChecks = 0;
        lastSuccessfulCheck = null;
        lastFailedCheck = null;
        lastErrorMessage = null;
        logger.info("Config Server health check statistics reset");
    }
}