package com.zamaz.mcp.controller.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resource limiting and throttling configuration for fair usage.
 */
@Configuration
public class AgenticFlowResourceLimitConfig {
    
    @Bean
    public RateLimiter organizationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100) // 100 requests per minute per organization
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        
        return RateLimiter.of("organizationRateLimiter", config);
    }
    
    @Bean
    public RateLimiter flowTypeRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(50) // 50 requests per minute per flow type
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        
        return RateLimiter.of("flowTypeRateLimiter", config);
    }
    
    @Bean
    public Bulkhead llmServiceBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(50) // Max 50 concurrent LLM calls
            .maxWaitDuration(Duration.ofSeconds(10))
            .build();
        
        return Bulkhead.of("llmServiceBulkhead", config);
    }
    
    @Bean
    public Bulkhead ragServiceBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(30) // Max 30 concurrent RAG operations
            .maxWaitDuration(Duration.ofSeconds(5))
            .build();
        
        return Bulkhead.of("ragServiceBulkhead", config);
    }
    
    @Bean
    public CircuitBreaker llmServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();
        
        return CircuitBreaker.of("llmServiceCircuitBreaker", config);
    }
    
    /**
     * Organization-specific resource limits.
     */
    @Bean
    public OrganizationResourceLimits organizationResourceLimits() {
        return new OrganizationResourceLimits();
    }
    
    public static class OrganizationResourceLimits {
        
        public int getMaxConcurrentFlows(String organizationId) {
            // Can be loaded from database based on organization tier
            return 10; // Default limit
        }
        
        public int getMaxFlowsPerDay(String organizationId) {
            // Can be loaded from database based on organization tier
            return 1000; // Default limit
        }
        
        public int getMaxTokensPerFlow(String organizationId) {
            // Can be loaded from database based on organization tier
            return 4000; // Default limit
        }
        
        public boolean isWithinLimits(String organizationId, int currentUsage) {
            return currentUsage < getMaxFlowsPerDay(organizationId);
        }
    }
}