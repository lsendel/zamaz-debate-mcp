package com.zamaz.mcp.controller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for template service integration.
 * Allows external configuration of template service connection settings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "template.service")
public class TemplateServiceProperties {
    
    /**
     * Template service base URL.
     * This should be configured via template.service.url property.
     */
    private String url;
    
    /**
     * Whether template service integration is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Connection timeout in milliseconds.
     */
    private int connectionTimeout = 5000;
    
    /**
     * Read timeout in milliseconds.
     */
    private int readTimeout = 10000;
    
    /**
     * Maximum number of retry attempts.
     */
    private int maxRetries = 3;
    
    /**
     * Circuit breaker configuration.
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    
    /**
     * Circuit breaker configuration.
     */
    @Data
    public static class CircuitBreakerConfig {
        /**
         * Whether circuit breaker is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Failure rate threshold (percentage).
         */
        private float failureRateThreshold = 50.0f;
        
        /**
         * Wait duration in seconds.
         */
        private int waitDurationInSeconds = 30;
        
        /**
         * Sliding window size.
         */
        private int slidingWindowSize = 10;
    }
}