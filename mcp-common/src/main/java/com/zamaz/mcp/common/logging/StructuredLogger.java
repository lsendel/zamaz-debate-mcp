package com.zamaz.mcp.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Structured logging utility for consistent log formatting across the application
 */
@Component
@RequiredArgsConstructor
public class StructuredLogger {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Log an event with structured data
     */
    public void logEvent(String loggerName, LogLevel level, String message, LogContext context) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        
        // Build structured log data
        Map<String, Object> logData = buildLogData(message, context);
        
        // Log with structured marker
        var marker = Markers.appendEntries(logData);
        
        switch (level) {
            case ERROR:
                if (context.getException() != null) {
                    logger.error(marker, message, context.getException());
                } else {
                    logger.error(marker, message);
                }
                break;
            case WARN:
                logger.warn(marker, message);
                break;
            case INFO:
                logger.info(marker, message);
                break;
            case DEBUG:
                logger.debug(marker, message);
                break;
            case TRACE:
                logger.trace(marker, message);
                break;
        }
    }
    
    /**
     * Build structured log data
     */
    private Map<String, Object> buildLogData(String message, LogContext context) {
        Map<String, Object> logData = new HashMap<>();
        
        // Timestamp
        logData.put("timestamp", Instant.now().toString());
        logData.put("message", message);
        
        // Request context
        addRequestContext(logData);
        
        // Security context
        addSecurityContext(logData);
        
        // Custom context
        if (context != null) {
            logData.put("organizationId", context.getOrganizationId());
            logData.put("userId", context.getUserId());
            logData.put("sessionId", context.getSessionId());
            logData.put("requestId", context.getRequestId());
            logData.put("correlationId", context.getCorrelationId());
            logData.put("component", context.getComponent());
            logData.put("operation", context.getOperation());
            logData.put("resourceType", context.getResourceType());
            logData.put("resourceId", context.getResourceId());
            logData.put("duration", context.getDuration());
            logData.put("statusCode", context.getStatusCode());
            
            if (context.getMetadata() != null) {
                logData.putAll(context.getMetadata());
            }
            
            if (context.getException() != null) {
                logData.put("errorType", context.getException().getClass().getSimpleName());
                logData.put("errorMessage", context.getException().getMessage());
                logData.put("stackTrace", getStackTrace(context.getException()));
            }
        }
        
        // Performance metrics
        addPerformanceMetrics(logData);
        
        return logData;
    }
    
    /**
     * Add request context to log data
     */
    private void addRequestContext(Map<String, Object> logData) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            logData.put("httpMethod", request.getMethod());
            logData.put("requestUri", request.getRequestURI());
            logData.put("queryString", request.getQueryString());
            logData.put("userAgent", request.getHeader("User-Agent"));
            logData.put("sourceIp", getClientIpAddress(request));
            logData.put("referer", request.getHeader("Referer"));
            
            // Add from MDC if available
            String requestId = MDC.get("requestId");
            if (requestId != null) {
                logData.put("requestId", requestId);
            }
            
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                logData.put("traceId", traceId);
            }
        }
    }
    
    /**
     * Add security context to log data
     */
    private void addSecurityContext(Map<String, Object> logData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            logData.put("authenticatedUser", auth.getName());
            logData.put("authType", auth.getClass().getSimpleName());
            
            if (auth.getAuthorities() != null) {
                logData.put("authorities", auth.getAuthorities().toString());
            }
        }
    }
    
    /**
     * Add performance metrics to log data
     */
    private void addPerformanceMetrics(Map<String, Object> logData) {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("usedMemoryMB", usedMemory / 1024 / 1024);
        jvm.put("totalMemoryMB", totalMemory / 1024 / 1024);
        jvm.put("maxMemoryMB", maxMemory / 1024 / 1024);
        jvm.put("freeMemoryMB", freeMemory / 1024 / 1024);
        jvm.put("memoryUsagePercent", (double) usedMemory / maxMemory * 100);
        jvm.put("availableProcessors", runtime.availableProcessors());
        
        logData.put("jvm", jvm);
        logData.put("threadName", Thread.currentThread().getName());
        logData.put("threadId", Thread.currentThread().getId());
    }
    
    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Convenience methods for different log levels
     */
    
    public void error(String loggerName, String message, LogContext context) {
        logEvent(loggerName, LogLevel.ERROR, message, context);
    }
    
    public void warn(String loggerName, String message, LogContext context) {
        logEvent(loggerName, LogLevel.WARN, message, context);
    }
    
    public void info(String loggerName, String message, LogContext context) {
        logEvent(loggerName, LogLevel.INFO, message, context);
    }
    
    public void debug(String loggerName, String message, LogContext context) {
        logEvent(loggerName, LogLevel.DEBUG, message, context);
    }
    
    public void trace(String loggerName, String message, LogContext context) {
        logEvent(loggerName, LogLevel.TRACE, message, context);
    }
    
    /**
     * Create a new log context builder
     */
    public static LogContext.LogContextBuilder context() {
        return LogContext.builder();
    }
    
    /**
     * Log levels
     */
    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG, TRACE
    }
}