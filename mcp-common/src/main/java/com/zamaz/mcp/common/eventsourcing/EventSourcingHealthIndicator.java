package com.zamaz.mcp.common.eventsourcing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator for event sourcing components
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventSourcingHealthIndicator implements HealthIndicator {
    
    private final EventStore eventStore;
    private static final String TEST_AGGREGATE_ID = "health-check-test";
    
    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Test event store connectivity
            boolean eventStoreHealthy = testEventStoreHealth(details);
            
            // Test snapshot store connectivity
            boolean snapshotStoreHealthy = testSnapshotStoreHealth(details);
            
            if (eventStoreHealthy && snapshotStoreHealthy) {
                return Health.up()
                    .withDetails(details)
                    .build();
            } else {
                return Health.down()
                    .withDetails(details)
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Event sourcing health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
    
    private boolean testEventStoreHealth(Map<String, Object> details) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test getting current version (should work even if aggregate doesn't exist)
            CompletableFuture<Long> versionFuture = eventStore.getCurrentVersion(TEST_AGGREGATE_ID);
            Long version = versionFuture.get(5, TimeUnit.SECONDS);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            details.put("eventStore.status", "UP");
            details.put("eventStore.responseTime", responseTime + "ms");
            details.put("eventStore.testAggregateVersion", version);
            
            log.debug("Event store health check passed in {}ms", responseTime);
            return true;
            
        } catch (Exception e) {
            details.put("eventStore.status", "DOWN");
            details.put("eventStore.error", e.getMessage());
            log.error("Event store health check failed", e);
            return false;
        }
    }
    
    private boolean testSnapshotStoreHealth(Map<String, Object> details) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test getting snapshot (should work even if snapshot doesn't exist)
            CompletableFuture<java.util.Optional<Snapshot>> snapshotFuture = 
                eventStore.getSnapshot(TEST_AGGREGATE_ID, 1);
            java.util.Optional<Snapshot> snapshot = snapshotFuture.get(5, TimeUnit.SECONDS);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            details.put("snapshotStore.status", "UP");
            details.put("snapshotStore.responseTime", responseTime + "ms");
            details.put("snapshotStore.testSnapshotExists", snapshot.isPresent());
            
            log.debug("Snapshot store health check passed in {}ms", responseTime);
            return true;
            
        } catch (Exception e) {
            details.put("snapshotStore.status", "DOWN");
            details.put("snapshotStore.error", e.getMessage());
            log.error("Snapshot store health check failed", e);
            return false;
        }
    }
}