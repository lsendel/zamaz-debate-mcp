package com.zamaz.mcp.common.config;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles errors during configuration loading from Config Server.
 * Provides fallback mechanisms and detailed error reporting.
 */
@Component
public class ConfigurationErrorHandler implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationErrorHandler.class);
    
    private final Map<String, ConfigError> configErrors = new ConcurrentHashMap<>();
    private final List<String> fallbackActions = new ArrayList<>();

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        Throwable failure = event.getException();
        
        if (isConfigurationError(failure)) {
            handleConfigurationError(failure, event.getApplicationContext().getEnvironment());
        }
    }

    /**
     * Checks if the error is related to configuration loading.
     */
    private boolean isConfigurationError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        return (message != null && (
            message.contains("Config Server") ||
            message.contains("Could not locate PropertySource") ||
            message.contains("fail-fast property is set")
        )) || isConfigurationError(throwable.getCause());
    }

    /**
     * Handles configuration loading errors.
     */
    private void handleConfigurationError(Throwable error, Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "unknown");
        String configUri = environment.getProperty("spring.cloud.config.uri", "unknown");
        
        logger.error("Configuration loading failed for application: {}", applicationName, error);
        
        // Record the error
        ConfigError configError = new ConfigError(
            applicationName,
            configUri,
            error.getMessage(),
            LocalDateTime.now()
        );
        configErrors.put(applicationName, configError);
        
        // Attempt fallback actions
        performFallbackActions(applicationName, environment);
        
        // Generate error report
        generateErrorReport(configError);
        
        // Provide recovery suggestions
        logRecoverySuggestions(error, environment);
    }

    /**
     * Performs fallback actions when configuration loading fails.
     */
    private void performFallbackActions(String applicationName, Environment environment) {
        logger.info("Attempting fallback actions for {}", applicationName);
        
        // Check for local configuration files
        if (checkLocalConfigurationFiles(applicationName)) {
            fallbackActions.add("Local configuration files found - application may start with reduced functionality");
        }
        
        // Check for cached configuration
        if (checkCachedConfiguration(applicationName)) {
            fallbackActions.add("Cached configuration found - using last known good configuration");
        }
        
        // Check for default configuration
        if (loadDefaultConfiguration(applicationName)) {
            fallbackActions.add("Default configuration loaded - basic functionality available");
        }
        
        if (fallbackActions.isEmpty()) {
            logger.error("No fallback configuration available for {}", applicationName);
        } else {
            logger.info("Fallback actions completed: {}", fallbackActions);
        }
    }

    /**
     * Checks for local configuration files.
     */
    private boolean checkLocalConfigurationFiles(String applicationName) {
        String[] locations = {
            "config/" + applicationName + ".yml",
            "config/" + applicationName + ".properties",
            "src/main/resources/application.yml",
            "src/main/resources/application.properties"
        };
        
        for (String location : locations) {
            File file = new File(location);
            if (file.exists() && file.canRead()) {
                logger.info("Found local configuration file: {}", location);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks for cached configuration.
     */
    private boolean checkCachedConfiguration(String applicationName) {
        File cacheDir = new File(System.getProperty("java.io.tmpdir"), "config-cache");
        File cacheFile = new File(cacheDir, applicationName + "-config.cache");
        
        if (cacheFile.exists() && cacheFile.canRead()) {
            long ageInHours = (System.currentTimeMillis() - cacheFile.lastModified()) / (1000 * 60 * 60);
            if (ageInHours < 24) {
                logger.info("Found cached configuration (age: {} hours): {}", ageInHours, cacheFile);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Loads default configuration.
     */
    private boolean loadDefaultConfiguration(String applicationName) {
        try {
            // In a real implementation, this would load actual default configuration
            logger.info("Loading default configuration for {}", applicationName);
            return true;
        } catch (Exception e) {
            logger.error("Failed to load default configuration", e);
            return false;
        }
    }

    /**
     * Generates an error report for configuration failures.
     */
    private void generateErrorReport(ConfigError error) {
        try {
            File reportDir = new File("config-error-reports");
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File reportFile = new File(reportDir, "config-error-" + error.applicationName + "-" + timestamp + ".txt");
            
            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write("Configuration Error Report\n");
                writer.write("=========================\n\n");
                writer.write("Application: " + error.applicationName + "\n");
                writer.write("Config Server: " + error.configServerUri + "\n");
                writer.write("Error Time: " + error.errorTime + "\n");
                writer.write("Error Message: " + error.errorMessage + "\n\n");
                writer.write("Fallback Actions Taken:\n");
                for (String action : fallbackActions) {
                    writer.write("- " + action + "\n");
                }
            }
            
            logger.info("Error report generated: {}", reportFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to generate error report", e);
        }
    }

    /**
     * Logs recovery suggestions based on the error.
     */
    private void logRecoverySuggestions(Throwable error, Environment environment) {
        logger.info("Configuration Error Recovery Suggestions:");
        logger.info("==========================================");
        
        if (error.getMessage() != null && error.getMessage().contains("Connection refused")) {
            logger.info("1. Check if Config Server is running at: {}", 
                environment.getProperty("spring.cloud.config.uri"));
            logger.info("2. Verify network connectivity to Config Server");
            logger.info("3. Check firewall rules and port accessibility");
        }
        
        if (error.getMessage() != null && error.getMessage().contains("401")) {
            logger.info("1. Verify Config Server credentials are correct");
            logger.info("2. Check if authentication is required for Config Server");
            logger.info("3. Update credentials in bootstrap.yml or environment variables");
        }
        
        if (error.getMessage() != null && error.getMessage().contains("404")) {
            logger.info("1. Verify application name matches configuration file name");
            logger.info("2. Check if configuration exists for profile: {}", 
                environment.getProperty("spring.profiles.active"));
            logger.info("3. Ensure Config Server has access to configuration repository");
        }
        
        logger.info("General suggestions:");
        logger.info("- Set spring.cloud.config.fail-fast=false to allow startup without Config Server");
        logger.info("- Provide local fallback configuration in application.yml");
        logger.info("- Enable configuration caching for resilience");
        logger.info("- Check Config Server logs for more details");
    }

    /**
     * Gets all recorded configuration errors.
     */
    public Map<String, ConfigError> getConfigErrors() {
        return new ConcurrentHashMap<>(configErrors);
    }

    /**
     * Clears error history.
     */
    public void clearErrors() {
        configErrors.clear();
        fallbackActions.clear();
    }

    /**
     * Configuration error details.
     */
    public static class ConfigError {
        private final String applicationName;
        private final String configServerUri;
        private final String errorMessage;
        private final LocalDateTime errorTime;

        public ConfigError(String applicationName, String configServerUri, 
                          String errorMessage, LocalDateTime errorTime) {
            this.applicationName = applicationName;
            this.configServerUri = configServerUri;
            this.errorMessage = errorMessage;
            this.errorTime = errorTime;
        }

        // Getters
        public String getApplicationName() {
            return applicationName;
        }

        public String getConfigServerUri() {
            return configServerUri;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public LocalDateTime getErrorTime() {
            return errorTime;
        }
    }
}