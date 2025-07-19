package com.zamaz.mcp.configserver.controller;

import com.zamaz.mcp.configserver.validator.SecurityConfigValidator;
import com.zamaz.mcp.configserver.validator.SecurityConfigValidator.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for security configuration management.
 * Provides endpoints for validation, refresh, and health checks.
 */
@RestController
@RequestMapping("/security-config")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigController.class);

    @Autowired
    private SecurityConfigValidator configValidator;

    @Autowired
    private ContextRefresher contextRefresher;

    /**
     * Validates the current security configuration
     */
    @GetMapping("/validate")
    public ResponseEntity<ConfigValidationResponse> validateConfiguration() {
        logger.info("Security configuration validation requested");
        
        ValidationResult result = configValidator.validateConfiguration();
        
        ConfigValidationResponse response = new ConfigValidationResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setValid(!result.hasErrors());
        response.setErrors(result.getErrors());
        response.setWarnings(result.getWarnings());
        
        HttpStatus status = result.hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        
        logger.info("Security configuration validation completed. Valid: {}", !result.hasErrors());
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Refreshes security configuration from the config server
     */
    @PostMapping("/refresh")
    public ResponseEntity<ConfigRefreshResponse> refreshConfiguration() {
        logger.info("Security configuration refresh requested");
        
        try {
            // Refresh the context to reload configuration
            Set<String> refreshedProperties = contextRefresher.refresh();
            
            // Validate the new configuration
            ValidationResult validationResult = configValidator.validateConfiguration();
            
            ConfigRefreshResponse response = new ConfigRefreshResponse();
            response.setTimestamp(LocalDateTime.now());
            response.setRefreshedProperties(refreshedProperties);
            response.setRefreshedCount(refreshedProperties.size());
            response.setValid(!validationResult.hasErrors());
            response.setErrors(validationResult.getErrors());
            response.setWarnings(validationResult.getWarnings());
            
            logger.info("Security configuration refresh completed. {} properties refreshed", 
                refreshedProperties.size());
            
            if (validationResult.hasErrors()) {
                logger.error("Configuration refresh resulted in invalid configuration");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to refresh security configuration", e);
            
            ConfigRefreshResponse errorResponse = new ConfigRefreshResponse();
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setValid(false);
            errorResponse.setErrors(Map.of("refresh", 
                java.util.List.of("Failed to refresh configuration: " + e.getMessage())));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for security configuration
     */
    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ConfigHealthResponse> checkHealth() {
        ValidationResult result = configValidator.validateConfiguration();
        
        ConfigHealthResponse response = new ConfigHealthResponse();
        response.setStatus(result.hasErrors() ? "DOWN" : "UP");
        response.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> details = new HashMap<>();
        details.put("configurationValid", !result.hasErrors());
        details.put("errorCount", result.getErrors().size());
        details.put("warningCount", result.getWarnings().size());
        
        response.setDetails(details);
        
        HttpStatus status = result.hasErrors() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK;
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Response class for configuration validation
     */
    public static class ConfigValidationResponse {
        private LocalDateTime timestamp;
        private boolean valid;
        private Map<String, java.util.List<String>> errors;
        private Map<String, java.util.List<String>> warnings;

        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public Map<String, java.util.List<String>> getErrors() { return errors; }
        public void setErrors(Map<String, java.util.List<String>> errors) { this.errors = errors; }
        public Map<String, java.util.List<String>> getWarnings() { return warnings; }
        public void setWarnings(Map<String, java.util.List<String>> warnings) { this.warnings = warnings; }
    }

    /**
     * Response class for configuration refresh
     */
    public static class ConfigRefreshResponse extends ConfigValidationResponse {
        private Set<String> refreshedProperties;
        private int refreshedCount;

        // Getters and setters
        public Set<String> getRefreshedProperties() { return refreshedProperties; }
        public void setRefreshedProperties(Set<String> refreshedProperties) { 
            this.refreshedProperties = refreshedProperties; 
        }
        public int getRefreshedCount() { return refreshedCount; }
        public void setRefreshedCount(int refreshedCount) { this.refreshedCount = refreshedCount; }
    }

    /**
     * Response class for health check
     */
    public static class ConfigHealthResponse {
        private String status;
        private LocalDateTime timestamp;
        private Map<String, Object> details;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}