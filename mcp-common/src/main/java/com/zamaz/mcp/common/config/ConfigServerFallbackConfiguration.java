package com.zamaz.mcp.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Fallback configuration when Spring Cloud Config Server is unavailable.
 * This allows services to start with local configuration files.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.config.fail-fast", havingValue = "false")
public class ConfigServerFallbackConfiguration implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigServerFallbackConfiguration.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        // Check if Config Server is available
        String configUri = environment.getProperty("spring.cloud.config.uri", "http://localhost:8888");
        if (!isConfigServerAvailable(configUri)) {
            logger.warn("Config Server at {} is not available. Loading fallback configuration.", configUri);
            loadFallbackConfiguration(environment);
        }
    }

    /**
     * Checks if the Config Server is available.
     */
    private boolean isConfigServerAvailable(String configUri) {
        try {
            // Simple check - in production, you might want to make an actual HTTP request
            java.net.URL url = new java.net.URL(configUri + "/actuator/health");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            logger.debug("Config Server health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Loads fallback configuration from local files.
     */
    private void loadFallbackConfiguration(ConfigurableEnvironment environment) {
        String appName = environment.getProperty("spring.application.name");
        String profile = environment.getProperty("spring.profiles.active", "default");
        
        // Load application-specific fallback configuration
        loadPropertiesFile(environment, "classpath:fallback/" + appName + ".properties");
        loadPropertiesFile(environment, "classpath:fallback/" + appName + "-" + profile + ".properties");
        
        // Load common fallback configuration
        loadPropertiesFile(environment, "classpath:fallback/application.properties");
        loadPropertiesFile(environment, "classpath:fallback/application-" + profile + ".properties");
    }

    /**
     * Loads a properties file and adds it to the environment.
     */
    private void loadPropertiesFile(ConfigurableEnvironment environment, String location) {
        try {
            Properties props = new Properties();
            String path = location.replace("classpath:", "");
            try (FileInputStream fis = new FileInputStream(
                getClass().getResource(path).getFile())) {
                props.load(fis);
                environment.getPropertySources().addLast(
                    new PropertiesPropertySource("fallback-" + location, props)
                );
                logger.info("Loaded fallback configuration from: {}", location);
            }
        } catch (IOException | NullPointerException e) {
            logger.debug("Fallback configuration file not found: {}", location);
        }
    }
}