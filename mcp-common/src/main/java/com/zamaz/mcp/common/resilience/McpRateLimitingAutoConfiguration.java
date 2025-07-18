package com.zamaz.mcp.common.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for MCP rate limiting components.
 * Automatically enables rate limiting when the feature is enabled in configuration.
 */
@Configuration
@EnableConfigurationProperties(McpRateLimitingConfiguration.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "mcp.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class McpRateLimitingAutoConfiguration {

    /**
     * Configure MCP rate limiting service if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public McpRateLimitingService mcpRateLimitingService(
            io.github.resilience4j.ratelimiter.RateLimiterRegistry rateLimiterRegistry,
            McpRateLimitingConfiguration rateLimitingConfig) {
        
        log.info("Auto-configuring MCP Rate Limiting Service");
        return new McpRateLimitingService(rateLimiterRegistry, rateLimitingConfig);
    }

    /**
     * Configure MCP rate limit aspect if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public McpRateLimitAspect mcpRateLimitAspect(
            McpRateLimitingService rateLimitingService,
            com.zamaz.mcp.common.error.McpErrorHandler mcpErrorHandler) {
        
        log.info("Auto-configuring MCP Rate Limit Aspect");
        return new McpRateLimitAspect(rateLimitingService, mcpErrorHandler);
    }

    /**
     * Configure MCP rate limiting configuration if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public McpRateLimitingConfiguration mcpRateLimitingConfiguration() {
        log.info("Auto-configuring MCP Rate Limiting Configuration with defaults");
        return new McpRateLimitingConfiguration().mcpRateLimitingConfiguration();
    }
}