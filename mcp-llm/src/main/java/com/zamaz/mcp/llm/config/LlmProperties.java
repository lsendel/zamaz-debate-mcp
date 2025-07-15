package com.zamaz.mcp.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    
    private Map<String, ProviderConfig> providers;
    private CacheConfig cache;
    private RateLimitingConfig rateLimiting;
    
    @Data
    public static class ProviderConfig {
        private boolean enabled;
        private String apiKey;
        private String baseUrl;
        private String defaultModel;
        private Integer maxTokens;
        private Duration timeout;
        private RetryConfig retry;
    }
    
    @Data
    public static class RetryConfig {
        private Integer maxAttempts;
        private Duration initialInterval;
        private Duration maxInterval;
        private Double multiplier;
    }
    
    @Data
    public static class CacheConfig {
        private boolean enabled;
        private Duration ttl;
        private Integer maxSize;
    }
    
    @Data
    public static class RateLimitingConfig {
        private boolean enabled;
        private Integer defaultRequestsPerMinute;
        private Map<String, Integer> providerLimits;
    }
}