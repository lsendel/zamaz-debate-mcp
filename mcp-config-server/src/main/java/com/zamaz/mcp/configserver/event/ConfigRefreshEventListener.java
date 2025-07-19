package com.zamaz.mcp.configserver.event;

import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.cloud.bus.event.EnvironmentChangeRemoteApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Listener for configuration refresh events.
 * Tracks and logs configuration refresh activities across the system.
 */
@Component
public class ConfigRefreshEventListener implements ApplicationListener<RefreshRemoteApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRefreshEventListener.class);
    
    private final AtomicLong refreshCount = new AtomicLong(0);
    private final Map<String, LocalDateTime> lastRefreshTimes = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(RefreshRemoteApplicationEvent event) {
        String originService = event.getOriginService();
        String destinationService = event.getDestinationService();
        
        logger.info("Configuration refresh event received. Origin: {}, Destination: {}", 
            originService, destinationService);
        
        // Track refresh statistics
        refreshCount.incrementAndGet();
        lastRefreshTimes.put(originService, LocalDateTime.now());
        
        // Log refresh details
        if ("**".equals(destinationService)) {
            logger.info("Broadcasting configuration refresh to all services");
        } else {
            logger.info("Sending configuration refresh to service: {}", destinationService);
        }
        
        // You can add custom logic here, such as:
        // - Sending notifications
        // - Updating metrics
        // - Triggering additional actions
    }

    /**
     * Gets the total number of refresh events.
     */
    public long getRefreshCount() {
        return refreshCount.get();
    }

    /**
     * Gets the last refresh time for a specific service.
     */
    public LocalDateTime getLastRefreshTime(String serviceName) {
        return lastRefreshTimes.get(serviceName);
    }

    /**
     * Gets all refresh statistics.
     */
    public Map<String, Object> getRefreshStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalRefreshCount", refreshCount.get());
        stats.put("lastRefreshTimes", new ConcurrentHashMap<>(lastRefreshTimes));
        stats.put("activeServices", lastRefreshTimes.size());
        return stats;
    }

    /**
     * Resets refresh statistics.
     */
    public void resetStatistics() {
        refreshCount.set(0);
        lastRefreshTimes.clear();
        logger.info("Refresh statistics have been reset");
    }
}