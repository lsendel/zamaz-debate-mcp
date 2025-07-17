package com.zamaz.mcp.common.infrastructure.config;

import java.util.Optional;

/**
 * Infrastructure service for accessing configuration.
 * This abstracts configuration access from the application and domain layers.
 */
public interface ConfigurationProvider {
    
    /**
     * Gets a configuration value as a string.
     * 
     * @param key the configuration key
     * @return the configuration value, or empty if not found
     */
    Optional<String> getString(String key);
    
    /**
     * Gets a configuration value as a string with a default.
     * 
     * @param key the configuration key
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    String getString(String key, String defaultValue);
    
    /**
     * Gets a configuration value as an integer.
     * 
     * @param key the configuration key
     * @return the configuration value, or empty if not found
     */
    Optional<Integer> getInteger(String key);
    
    /**
     * Gets a configuration value as an integer with a default.
     * 
     * @param key the configuration key
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    Integer getInteger(String key, Integer defaultValue);
    
    /**
     * Gets a configuration value as a boolean.
     * 
     * @param key the configuration key
     * @return the configuration value, or empty if not found
     */
    Optional<Boolean> getBoolean(String key);
    
    /**
     * Gets a configuration value as a boolean with a default.
     * 
     * @param key the configuration key
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    Boolean getBoolean(String key, Boolean defaultValue);
    
    /**
     * Checks if a configuration key exists.
     * 
     * @param key the configuration key
     * @return true if the key exists
     */
    boolean hasKey(String key);
}