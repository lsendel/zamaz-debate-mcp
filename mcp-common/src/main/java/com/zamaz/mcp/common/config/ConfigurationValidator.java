package com.zamaz.mcp.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates configuration properties on startup and after refresh.
 * Ensures that all required configuration is present and valid.
 */
@Component
@RefreshScope
public class ConfigurationValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    @Autowired(required = false)
    private DatabaseConfigProperties databaseConfig;

    @Autowired(required = false)
    private SecurityConfigProperties securityConfig;

    @Autowired(required = false)
    private MonitoringConfigProperties monitoringConfig;

    @Autowired
    private Validator validator;

    private final Map<String, List<String>> validationErrors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Configuration validator initialized");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Validating configuration on application startup");
        validateAllConfigurations();
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefreshEvent(RefreshScopeRefreshedEvent event) {
        logger.info("Validating configuration after refresh");
        validateAllConfigurations();
    }

    /**
     * Validates all configuration properties.
     */
    public void validateAllConfigurations() {
        validationErrors.clear();
        
        // Validate database configuration
        if (databaseConfig != null) {
            validateDatabaseConfig();
        } else {
            logger.warn("Database configuration not found");
        }
        
        // Validate security configuration
        if (securityConfig != null) {
            validateSecurityConfig();
        } else {
            logger.warn("Security configuration not found");
        }
        
        // Validate monitoring configuration
        if (monitoringConfig != null) {
            validateMonitoringConfig();
        } else {
            logger.warn("Monitoring configuration not found");
        }
        
        // Log validation results
        if (!validationErrors.isEmpty()) {
            logger.error("Configuration validation failed with {} errors", validationErrors.size());
            validationErrors.forEach((config, errors) -> {
                logger.error("Configuration '{}' has {} validation errors:", config, errors.size());
                errors.forEach(error -> logger.error("  - {}", error));
            });
            
            // In production, you might want to prevent startup on critical errors
            // throw new ConfigurationException("Configuration validation failed");
        } else {
            logger.info("All configuration validation passed");
        }
    }

    /**
     * Validates database configuration.
     */
    private void validateDatabaseConfig() {
        logger.debug("Validating database configuration");
        
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            databaseConfig, "databaseConfig"
        );
        
        validator.validate(databaseConfig, bindingResult);
        
        if (bindingResult.hasErrors()) {
            List<String> errors = new ArrayList<>();
            bindingResult.getAllErrors().forEach(error -> 
                errors.add(error.getDefaultMessage())
            );
            validationErrors.put("database", errors);
        }
        
        // Custom validation
        validateDatabaseCustomRules();
    }

    /**
     * Custom database configuration validation rules.
     */
    private void validateDatabaseCustomRules() {
        List<String> customErrors = validationErrors.computeIfAbsent("database", k -> new ArrayList<>());
        
        // Check pool size consistency
        if (databaseConfig.getMaxPoolSize() < databaseConfig.getMinPoolSize()) {
            customErrors.add("Max pool size must be greater than or equal to min pool size");
        }
        
        // Check timeout consistency
        if (databaseConfig.getIdleTimeout() > databaseConfig.getMaxLifetime()) {
            customErrors.add("Idle timeout must be less than max lifetime");
        }
        
        // Check connection URL format
        String url = databaseConfig.getUrl();
        if (url != null && !url.startsWith("jdbc:")) {
            customErrors.add("Database URL must start with 'jdbc:'");
        }
        
        // Validate driver class
        try {
            Class.forName(databaseConfig.getDriverClassName());
        } catch (ClassNotFoundException e) {
            customErrors.add("Database driver class not found: " + databaseConfig.getDriverClassName());
        }
    }

    /**
     * Validates security configuration.
     */
    private void validateSecurityConfig() {
        logger.debug("Validating security configuration");
        
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            securityConfig, "securityConfig"
        );
        
        validator.validate(securityConfig, bindingResult);
        
        if (bindingResult.hasErrors()) {
            List<String> errors = new ArrayList<>();
            bindingResult.getAllErrors().forEach(error -> 
                errors.add(error.getDefaultMessage())
            );
            validationErrors.put("security", errors);
        }
        
        // Validate nested objects
        validateJwtConfig();
        validateCorsConfig();
    }

    /**
     * Validates JWT configuration.
     */
    private void validateJwtConfig() {
        SecurityConfigProperties.Jwt jwt = securityConfig.getJwt();
        List<String> jwtErrors = validationErrors.computeIfAbsent("security.jwt", k -> new ArrayList<>());
        
        // Check secret strength
        if (jwt.getSecret() != null && jwt.getSecret().length() < 32) {
            jwtErrors.add("JWT secret key should be at least 32 characters for security");
        }
        
        // Check expiration consistency
        if (jwt.getExpiration() > jwt.getRefreshExpiration()) {
            jwtErrors.add("Access token expiration should be less than refresh token expiration");
        }
        
        // Validate algorithm
        String algorithm = jwt.getAlgorithm();
        if (!isValidJwtAlgorithm(algorithm)) {
            jwtErrors.add("Invalid JWT algorithm: " + algorithm);
        }
    }

    /**
     * Validates CORS configuration.
     */
    private void validateCorsConfig() {
        SecurityConfigProperties.Cors cors = securityConfig.getCors();
        List<String> corsErrors = validationErrors.computeIfAbsent("security.cors", k -> new ArrayList<>());
        
        // Check for wildcard with credentials
        if (cors.isAllowCredentials() && cors.getAllowedOrigins().contains("*")) {
            corsErrors.add("Cannot use wildcard origin '*' when credentials are allowed");
        }
        
        // Check for empty allowed methods
        if (cors.getAllowedMethods().isEmpty()) {
            corsErrors.add("At least one HTTP method must be allowed for CORS");
        }
    }

    /**
     * Validates monitoring configuration.
     */
    private void validateMonitoringConfig() {
        logger.debug("Validating monitoring configuration");
        
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            monitoringConfig, "monitoringConfig"
        );
        
        validator.validate(monitoringConfig, bindingResult);
        
        if (bindingResult.hasErrors()) {
            List<String> errors = new ArrayList<>();
            bindingResult.getAllErrors().forEach(error -> 
                errors.add(error.getDefaultMessage())
            );
            validationErrors.put("monitoring", errors);
        }
        
        // Custom monitoring validation
        validateMonitoringCustomRules();
    }

    /**
     * Custom monitoring configuration validation rules.
     */
    private void validateMonitoringCustomRules() {
        List<String> customErrors = validationErrors.computeIfAbsent("monitoring", k -> new ArrayList<>());
        
        // Check endpoint exposure
        if (monitoringConfig.isEnabled() && monitoringConfig.getExposedEndpoints().isEmpty()) {
            customErrors.add("At least one endpoint must be exposed when monitoring is enabled");
        }
        
        // Validate tracing configuration
        MonitoringConfigProperties.Tracing tracing = monitoringConfig.getTracing();
        if (tracing.isEnabled() && tracing.getEndpoint() == null) {
            customErrors.add("Tracing endpoint must be configured when tracing is enabled");
        }
        
        // Validate alerting thresholds
        MonitoringConfigProperties.Alerting.Thresholds thresholds = 
            monitoringConfig.getAlerting().getThresholds();
        if (thresholds.getErrorRate() < 0 || thresholds.getErrorRate() > 1) {
            customErrors.add("Error rate threshold must be between 0 and 1");
        }
    }

    /**
     * Checks if a JWT algorithm is valid.
     */
    private boolean isValidJwtAlgorithm(String algorithm) {
        return algorithm != null && (
            algorithm.startsWith("HS") || 
            algorithm.startsWith("RS") || 
            algorithm.startsWith("ES")
        );
    }

    /**
     * Gets all validation errors.
     */
    public Map<String, List<String>> getValidationErrors() {
        return new ConcurrentHashMap<>(validationErrors);
    }

    /**
     * Checks if configuration is valid.
     */
    public boolean isConfigurationValid() {
        return validationErrors.isEmpty();
    }

    /**
     * Custom exception for configuration validation errors.
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}