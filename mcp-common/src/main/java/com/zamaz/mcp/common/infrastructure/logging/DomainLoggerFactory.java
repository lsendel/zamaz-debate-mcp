package com.zamaz.mcp.common.infrastructure.logging;

/**
 * Factory for creating domain loggers.
 * This allows the domain and application layers to create loggers
 * without depending on specific logging frameworks.
 */
public interface DomainLoggerFactory {
    
    /**
     * Creates a logger for a specific class.
     * 
     * @param clazz the class requesting the logger
     * @return a domain logger
     */
    DomainLogger getLogger(Class<?> clazz);
    
    /**
     * Creates a logger with a specific name.
     * 
     * @param name the logger name
     * @return a domain logger
     */
    DomainLogger getLogger(String name);
}