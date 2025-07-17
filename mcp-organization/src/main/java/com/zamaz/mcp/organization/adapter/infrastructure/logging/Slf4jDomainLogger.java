package com.zamaz.mcp.organization.adapter.infrastructure.logging;

import com.zamaz.mcp.common.infrastructure.logging.DomainLogger;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * SLF4J implementation of DomainLogger.
 * Provides structured logging using SLF4J with MDC support.
 */
public class Slf4jDomainLogger implements DomainLogger {
    
    private final Logger slf4jLogger;
    private final Map<String, Object> context;
    
    public Slf4jDomainLogger(Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
        this.context = new HashMap<>();
    }
    
    private Slf4jDomainLogger(Logger slf4jLogger, Map<String, Object> context) {
        this.slf4jLogger = slf4jLogger;
        this.context = new HashMap<>(context);
    }
    
    @Override
    public void debug(String message, Object... args) {
        withMDC(() -> slf4jLogger.debug(message, args));
    }
    
    @Override
    public void info(String message, Object... args) {
        withMDC(() -> slf4jLogger.info(message, args));
    }
    
    @Override
    public void warn(String message, Object... args) {
        withMDC(() -> slf4jLogger.warn(message, args));
    }
    
    @Override
    public void error(String message, Object... args) {
        withMDC(() -> slf4jLogger.error(message, args));
    }
    
    @Override
    public void error(String message, Throwable throwable, Object... args) {
        withMDC(() -> slf4jLogger.error(String.format(message, args), throwable));
    }
    
    @Override
    public DomainLogger withContext(Map<String, Object> additionalContext) {
        Map<String, Object> newContext = new HashMap<>(this.context);
        newContext.putAll(additionalContext);
        return new Slf4jDomainLogger(slf4jLogger, newContext);
    }
    
    @Override
    public DomainLogger withContext(String key, Object value) {
        Map<String, Object> newContext = new HashMap<>(this.context);
        newContext.put(key, value);
        return new Slf4jDomainLogger(slf4jLogger, newContext);
    }
    
    /**
     * Executes the logging operation with MDC context.
     */
    private void withMDC(Runnable loggingOperation) {
        // Save current MDC
        Map<String, String> originalMDC = MDC.getCopyOfContextMap();
        
        try {
            // Add our context to MDC
            context.forEach((key, value) -> 
                MDC.put(key, value != null ? value.toString() : "null")
            );
            
            // Execute logging
            loggingOperation.run();
            
        } finally {
            // Restore original MDC
            MDC.clear();
            if (originalMDC != null) {
                MDC.setContextMap(originalMDC);
            }
        }
    }
}