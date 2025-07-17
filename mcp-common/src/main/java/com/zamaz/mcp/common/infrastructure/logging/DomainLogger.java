package com.zamaz.mcp.common.infrastructure.logging;

import java.util.Map;

/**
 * Logging interface for domain and application layers.
 * This abstracts logging from specific logging frameworks.
 */
public interface DomainLogger {
    
    /**
     * Logs a debug message.
     * 
     * @param message the message to log
     * @param args message arguments for formatting
     */
    void debug(String message, Object... args);
    
    /**
     * Logs an info message.
     * 
     * @param message the message to log
     * @param args message arguments for formatting
     */
    void info(String message, Object... args);
    
    /**
     * Logs a warning message.
     * 
     * @param message the message to log
     * @param args message arguments for formatting
     */
    void warn(String message, Object... args);
    
    /**
     * Logs an error message.
     * 
     * @param message the message to log
     * @param args message arguments for formatting
     */
    void error(String message, Object... args);
    
    /**
     * Logs an error message with an exception.
     * 
     * @param message the message to log
     * @param throwable the exception to log
     * @param args message arguments for formatting
     */
    void error(String message, Throwable throwable, Object... args);
    
    /**
     * Creates a logger with additional context.
     * 
     * @param context key-value pairs to add to all log messages
     * @return a new logger with the added context
     */
    DomainLogger withContext(Map<String, Object> context);
    
    /**
     * Creates a logger with a single context value.
     * 
     * @param key the context key
     * @param value the context value
     * @return a new logger with the added context
     */
    DomainLogger withContext(String key, Object value);
}