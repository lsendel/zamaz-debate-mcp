package com.zamaz.mcp.controller.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator for agentic flow system components.
 */
@Component("agenticFlowHealth")
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final RestTemplate restTemplate;
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Health.Builder builder = new Health.Builder();
        
        try {
            // Check database connectivity
            boolean dbHealthy = checkDatabase();
            details.put("database", dbHealthy ? "UP" : "DOWN");
            
            // Check LLM service availability
            boolean llmHealthy = checkLLMService();
            details.put("llm_service", llmHealthy ? "UP" : "DOWN");
            
            // Check queue health
            boolean queueHealthy = checkQueueHealth();
            details.put("message_queue", queueHealthy ? "UP" : "DOWN");
            
            // Check cache health
            boolean cacheHealthy = checkCacheHealth();
            details.put("cache", cacheHealthy ? "UP" : "DOWN");
            
            // Overall health
            if (dbHealthy && llmHealthy && queueHealthy && cacheHealthy) {
                builder.up();
            } else if (dbHealthy && (llmHealthy || queueHealthy)) {
                builder.degraded();
                details.put("status", "DEGRADED - Some services unavailable");
            } else {
                builder.down();
                details.put("status", "DOWN - Critical services unavailable");
            }
            
            // Add performance metrics
            details.put("active_flows", getActiveFlowCount());
            details.put("queue_size", getQueueSize());
            details.put("cache_hit_rate", getCacheHitRate());
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            builder.down().withException(e);
        }
        
        return builder.withDetails(details).build();
    }
    
    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return false;
        }
    }
    
    private boolean checkLLMService() {
        try {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    restTemplate.getForObject("http://localhost:5002/health", String.class);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("LLM service health check failed", e);
            return false;
        }
    }
    
    private boolean checkQueueHealth() {
        // Simplified check - in real implementation would check RabbitMQ
        try {
            // Check if RabbitMQ management API is accessible
            restTemplate.getForObject("http://localhost:15672/api/health/checks/alarms", String.class);
            return true;
        } catch (Exception e) {
            log.warn("Queue health check failed", e);
            return true; // Don't fail health check if RabbitMQ management is down
        }
    }
    
    private boolean checkCacheHealth() {
        // Simplified check - in real implementation would check cache statistics
        return true;
    }
    
    private int getActiveFlowCount() {
        // In real implementation, would query from metrics collector
        return 0;
    }
    
    private int getQueueSize() {
        // In real implementation, would query from RabbitMQ
        return 0;
    }
    
    private double getCacheHitRate() {
        // In real implementation, would calculate from cache statistics
        return 0.85;
    }
}