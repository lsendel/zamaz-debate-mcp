package com.zamaz.mcp.common.resilience;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP-specific rate limiting configuration.
 * Provides intelligent defaults for different types of MCP operations.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.rate-limiting")
@Data
@Slf4j
public class McpRateLimitingConfiguration {

    /**
     * Whether MCP rate limiting is enabled
     */
    private boolean enabled = true;

    /**
     * Global default rate limits
     */
    private GlobalRateLimits global = new GlobalRateLimits();

    /**
     * Service-specific rate limits
     */
    private Map<String, ServiceRateLimits> services = new HashMap<>();

    /**
     * Tool-specific rate limits
     */
    private Map<String, ToolRateLimits> tools = new HashMap<>();

    /**
     * User tier-based rate limits
     */
    private Map<String, TierRateLimits> tiers = new HashMap<>();

    /**
     * Whether to apply organization-level rate limiting
     */
    private boolean organizationLevelLimiting = true;

    /**
     * Whether to apply user-level rate limiting
     */
    private boolean userLevelLimiting = true;

    /**
     * Whether to apply IP-level rate limiting for unauthenticated requests
     */
    private boolean ipLevelLimiting = true;

    /**
     * Initialize default configurations
     */
    @Bean
    public McpRateLimitingConfiguration mcpRateLimitingConfiguration() {
        initializeDefaultConfigurations();
        log.info("MCP Rate Limiting Configuration initialized with {} services, {} tools, {} tiers", 
                services.size(), tools.size(), tiers.size());
        return this;
    }

    /**
     * Initialize default rate limiting configurations for MCP services and tools.
     */
    private void initializeDefaultConfigurations() {
        // Initialize default service configurations
        initializeDefaultServiceConfigurations();
        
        // Initialize default tool configurations
        initializeDefaultToolConfigurations();
        
        // Initialize default tier configurations
        initializeDefaultTierConfigurations();
    }

    /**
     * Initialize default service configurations.
     */
    private void initializeDefaultServiceConfigurations() {
        if (!services.containsKey("organization")) {
            services.put("organization", new ServiceRateLimits(100, 1, 5)); // 100 req/sec, 1 sec refresh, 5 sec timeout
        }
        
        if (!services.containsKey("context")) {
            services.put("context", new ServiceRateLimits(50, 1, 10)); // Context operations can be expensive
        }
        
        if (!services.containsKey("llm")) {
            services.put("llm", new ServiceRateLimits(10, 60, 30)); // LLM is expensive - 10 req/min
        }
        
        if (!services.containsKey("controller")) {
            services.put("controller", new ServiceRateLimits(30, 1, 10)); // Debate operations
        }
        
        if (!services.containsKey("rag")) {
            services.put("rag", new ServiceRateLimits(20, 1, 15)); // RAG operations can be expensive
        }
    }

    /**
     * Initialize default tool configurations.
     */
    private void initializeDefaultToolConfigurations() {
        // Organization tools
        if (!tools.containsKey("create_organization")) {
            tools.put("create_organization", new ToolRateLimits(5, 60, 10)); // Very restrictive
        }
        
        if (!tools.containsKey("delete_organization")) {
            tools.put("delete_organization", new ToolRateLimits(1, 300, 10)); // Extremely restrictive
        }
        
        // LLM tools - most expensive operations
        if (!tools.containsKey("generate_completion")) {
            tools.put("generate_completion", new ToolRateLimits(5, 60, 30)); // 5 completions per minute
        }
        
        // Context tools
        if (!tools.containsKey("create_context")) {
            tools.put("create_context", new ToolRateLimits(20, 60, 10));
        }
        
        if (!tools.containsKey("append_message")) {
            tools.put("append_message", new ToolRateLimits(100, 60, 5)); // Higher limit for messaging
        }
        
        // RAG tools
        if (!tools.containsKey("search_documents")) {
            tools.put("search_documents", new ToolRateLimits(30, 60, 10));
        }
        
        if (!tools.containsKey("generate_rag_context")) {
            tools.put("generate_rag_context", new ToolRateLimits(10, 60, 15)); // Expensive operation
        }
    }

    /**
     * Initialize default tier configurations.
     */
    private void initializeDefaultTierConfigurations() {
        if (!tiers.containsKey("free")) {
            tiers.put("free", new TierRateLimits(50, 60, 5, 1.0)); // Basic tier
        }
        
        if (!tiers.containsKey("pro")) {
            tiers.put("pro", new TierRateLimits(200, 60, 10, 3.0)); // 3x multiplier
        }
        
        if (!tiers.containsKey("enterprise")) {
            tiers.put("enterprise", new TierRateLimits(1000, 60, 5, 10.0)); // 10x multiplier
        }
        
        if (!tiers.containsKey("admin")) {
            tiers.put("admin", new TierRateLimits(10000, 60, 1, 100.0)); // Minimal restrictions
        }
    }

    /**
     * Get effective rate limits for a specific context.
     */
    public EffectiveRateLimits getEffectiveRateLimits(String serviceName, String toolName, 
                                                     String userTier, String organizationTier) {
        // Start with global defaults
        EffectiveRateLimits effective = new EffectiveRateLimits(
            global.limitForPeriod,
            global.limitRefreshPeriodSeconds,
            global.timeoutDurationSeconds
        );

        // Apply service-specific limits
        ServiceRateLimits serviceConfig = services.get(serviceName);
        if (serviceConfig != null) {
            effective.applyServiceLimits(serviceConfig);
        }

        // Apply tool-specific limits (most restrictive)
        ToolRateLimits toolConfig = tools.get(toolName);
        if (toolConfig != null) {
            effective.applyToolLimits(toolConfig);
        }

        // Apply tier multipliers
        double multiplier = 1.0;
        
        // User tier multiplier
        TierRateLimits userTierConfig = tiers.get(userTier);
        if (userTierConfig != null) {
            multiplier *= userTierConfig.multiplier;
        }
        
        // Organization tier multiplier (use higher of user or org tier)
        TierRateLimits orgTierConfig = tiers.get(organizationTier);
        if (orgTierConfig != null) {
            multiplier = Math.max(multiplier, orgTierConfig.multiplier);
        }

        effective.applyMultiplier(multiplier);
        
        return effective;
    }

    // Configuration classes
    @Data
    public static class GlobalRateLimits {
        private int limitForPeriod = 100;
        private int limitRefreshPeriodSeconds = 1;
        private int timeoutDurationSeconds = 5;
    }

    @Data
    public static class ServiceRateLimits {
        private int limitForPeriod;
        private int limitRefreshPeriodSeconds;
        private int timeoutDurationSeconds;

        public ServiceRateLimits() {}
        
        public ServiceRateLimits(int limitForPeriod, int limitRefreshPeriodSeconds, int timeoutDurationSeconds) {
            this.limitForPeriod = limitForPeriod;
            this.limitRefreshPeriodSeconds = limitRefreshPeriodSeconds;
            this.timeoutDurationSeconds = timeoutDurationSeconds;
        }
    }

    @Data
    public static class ToolRateLimits {
        private int limitForPeriod;
        private int limitRefreshPeriodSeconds;
        private int timeoutDurationSeconds;

        public ToolRateLimits() {}
        
        public ToolRateLimits(int limitForPeriod, int limitRefreshPeriodSeconds, int timeoutDurationSeconds) {
            this.limitForPeriod = limitForPeriod;
            this.limitRefreshPeriodSeconds = limitRefreshPeriodSeconds;
            this.timeoutDurationSeconds = timeoutDurationSeconds;
        }
    }

    @Data
    public static class TierRateLimits {
        private int limitForPeriod;
        private int limitRefreshPeriodSeconds;
        private int timeoutDurationSeconds;
        private double multiplier;

        public TierRateLimits() {}
        
        public TierRateLimits(int limitForPeriod, int limitRefreshPeriodSeconds, 
                             int timeoutDurationSeconds, double multiplier) {
            this.limitForPeriod = limitForPeriod;
            this.limitRefreshPeriodSeconds = limitRefreshPeriodSeconds;
            this.timeoutDurationSeconds = timeoutDurationSeconds;
            this.multiplier = multiplier;
        }
    }

    /**
     * Effective rate limits after all configurations are applied.
     */
    @Data
    public static class EffectiveRateLimits {
        private int limitForPeriod;
        private int limitRefreshPeriodSeconds;
        private int timeoutDurationSeconds;

        public EffectiveRateLimits(int limitForPeriod, int limitRefreshPeriodSeconds, int timeoutDurationSeconds) {
            this.limitForPeriod = limitForPeriod;
            this.limitRefreshPeriodSeconds = limitRefreshPeriodSeconds;
            this.timeoutDurationSeconds = timeoutDurationSeconds;
        }

        public void applyServiceLimits(ServiceRateLimits serviceConfig) {
            // Take the more restrictive limits
            this.limitForPeriod = Math.min(this.limitForPeriod, serviceConfig.limitForPeriod);
            this.limitRefreshPeriodSeconds = Math.max(this.limitRefreshPeriodSeconds, serviceConfig.limitRefreshPeriodSeconds);
            this.timeoutDurationSeconds = Math.min(this.timeoutDurationSeconds, serviceConfig.timeoutDurationSeconds);
        }

        public void applyToolLimits(ToolRateLimits toolConfig) {
            // Tool limits are most specific and override others
            this.limitForPeriod = toolConfig.limitForPeriod;
            this.limitRefreshPeriodSeconds = toolConfig.limitRefreshPeriodSeconds;
            this.timeoutDurationSeconds = toolConfig.timeoutDurationSeconds;
        }

        public void applyMultiplier(double multiplier) {
            // Apply multiplier to increase limits for higher tiers
            this.limitForPeriod = (int) Math.ceil(this.limitForPeriod * multiplier);
        }
    }
}