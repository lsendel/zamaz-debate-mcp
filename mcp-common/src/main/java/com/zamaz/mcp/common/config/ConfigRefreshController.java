package com.zamaz.mcp.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for manual configuration refresh operations.
 * Provides endpoints to trigger configuration refresh without Spring Cloud Bus.
 */
@RestController
@RequestMapping("/actuator/refresh")
@RefreshScope
@ConditionalOnProperty(name = "mcp.config.refresh.enabled", havingValue = "true", matchIfMissing = true)
public class ConfigRefreshController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRefreshController.class);

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    /**
     * Manually triggers a configuration refresh.
     * This endpoint should be secured and only accessible to administrators.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefreshResponse> refresh() {
        if (contextRefresher == null) {
            logger.warn("Context refresher not available. Configuration refresh is not supported.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RefreshResponse(false, "Configuration refresh not available", null));
        }

        try {
            logger.info("Initiating configuration refresh");
            Set<String> refreshedProperties = contextRefresher.refresh();
            
            logger.info("Configuration refresh completed. {} properties were refreshed", 
                refreshedProperties.size());
            
            return ResponseEntity.ok(new RefreshResponse(
                true, 
                "Configuration refreshed successfully", 
                refreshedProperties
            ));
        } catch (Exception e) {
            logger.error("Failed to refresh configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RefreshResponse(false, "Failed to refresh configuration: " + e.getMessage(), null));
        }
    }

    /**
     * Gets the current refresh status and statistics.
     */
    @GetMapping("/status")
    public ResponseEntity<RefreshStatus> getRefreshStatus() {
        RefreshStatus status = new RefreshStatus();
        status.setRefreshEnabled(contextRefresher != null);
        status.setLastRefreshTime(getLastRefreshTime());
        status.setRefreshCount(getRefreshCount());
        
        return ResponseEntity.ok(status);
    }

    /**
     * Selectively refreshes specific configuration properties.
     */
    @PostMapping("/selective")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefreshResponse> selectiveRefresh(@RequestBody SelectiveRefreshRequest request) {
        if (contextRefresher == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RefreshResponse(false, "Configuration refresh not available", null));
        }

        try {
            logger.info("Initiating selective configuration refresh for properties: {}", 
                request.getProperties());
            
            // In a real implementation, this would refresh only specific properties
            // For now, we'll do a full refresh
            Set<String> refreshedProperties = contextRefresher.refresh();
            
            return ResponseEntity.ok(new RefreshResponse(
                true, 
                "Selective refresh completed", 
                refreshedProperties
            ));
        } catch (Exception e) {
            logger.error("Failed to perform selective refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RefreshResponse(false, "Failed to refresh: " + e.getMessage(), null));
        }
    }

    // Helper methods to track refresh statistics
    private static long lastRefreshTime = 0;
    private static long refreshCount = 0;

    private long getLastRefreshTime() {
        return lastRefreshTime;
    }

    private long getRefreshCount() {
        return refreshCount;
    }

    /**
     * Response class for refresh operations
     */
    public static class RefreshResponse {
        private boolean success;
        private String message;
        private Set<String> refreshedProperties;
        private long timestamp;

        public RefreshResponse(boolean success, String message, Set<String> refreshedProperties) {
            this.success = success;
            this.message = message;
            this.refreshedProperties = refreshedProperties;
            this.timestamp = System.currentTimeMillis();
            
            if (success) {
                lastRefreshTime = this.timestamp;
                refreshCount++;
            }
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Set<String> getRefreshedProperties() {
            return refreshedProperties;
        }

        public void setRefreshedProperties(Set<String> refreshedProperties) {
            this.refreshedProperties = refreshedProperties;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Status information for refresh operations
     */
    public static class RefreshStatus {
        private boolean refreshEnabled;
        private long lastRefreshTime;
        private long refreshCount;
        private Map<String, Object> metadata = new HashMap<>();

        // Getters and setters
        public boolean isRefreshEnabled() {
            return refreshEnabled;
        }

        public void setRefreshEnabled(boolean refreshEnabled) {
            this.refreshEnabled = refreshEnabled;
        }

        public long getLastRefreshTime() {
            return lastRefreshTime;
        }

        public void setLastRefreshTime(long lastRefreshTime) {
            this.lastRefreshTime = lastRefreshTime;
        }

        public long getRefreshCount() {
            return refreshCount;
        }

        public void setRefreshCount(long refreshCount) {
            this.refreshCount = refreshCount;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * Request for selective refresh
     */
    public static class SelectiveRefreshRequest {
        private Set<String> properties;
        private boolean force = false;

        // Getters and setters
        public Set<String> getProperties() {
            return properties;
        }

        public void setProperties(Set<String> properties) {
            this.properties = properties;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }
}