package com.zamaz.mcp.common.infrastructure.logging;

import org.springframework.stereotype.Component;

/**
 * Factory for creating structured loggers.
 * Provides a bridge between the domain logging interface and the infrastructure
 * logging implementation.
 */
@Component
public class StructuredLoggerFactory {

    /**
     * Creates a structured logger for a specific class.
     * 
     * @param clazz the class requesting the logger
     * @return a structured logger
     */
    public StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLoggerImpl(clazz);
    }

    /**
     * Creates a structured logger with a specific name.
     * 
     * @param name the logger name
     * @return a structured logger
     */
    public StructuredLogger getLogger(String name) {
        return new StructuredLoggerImpl(name);
    }
}