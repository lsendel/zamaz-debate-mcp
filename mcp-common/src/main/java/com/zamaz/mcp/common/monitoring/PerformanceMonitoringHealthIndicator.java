package com.zamaz.mcp.common.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for performance monitoring system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringHealthIndicator implements HealthIndicator {
    
    private final PerformanceMonitoringService monitoringService;
    
    @Override
    public Health health() {
        try {
            // Get current performance snapshot
            var snapshot = monitoringService.getCurrentPerformanceSnapshot();
            
            // Build health details
            Map<String, Object> details = new HashMap<>();
            details.put("cpu_usage", String.format("%.2f%%", snapshot.getCpuUsage() * 100));
            details.put("memory_usage", String.format("%.2f%%", 
                (double) snapshot.getMemoryUsed() / snapshot.getMemoryMax() * 100));
            details.put("thread_count", snapshot.getThreadCount());
            details.put("system_load", snapshot.getSystemLoad());
            details.put("health_score", String.format("%.2f", snapshot.getHealthScore()));
            
            // Determine health status
            if (snapshot.getHealthScore() < 0.3) {
                return Health.down()
                    .withDetails(details)
                    .withDetail("status", "System performance is critical")
                    .build();
            } else if (snapshot.getHealthScore() < 0.7) {
                return Health.up()
                    .withDetails(details)
                    .withDetail("status", "System performance is degraded")
                    .build();
            } else {
                return Health.up()
                    .withDetails(details)
                    .withDetail("status", "System performance is healthy")
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error checking performance monitoring health", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("status", "Performance monitoring system is unavailable")
                .build();
        }
    }
}