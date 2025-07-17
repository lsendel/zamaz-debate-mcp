package com.zamaz.mcp.common.logging;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Context object for structured logging
 */
@Data
@Builder
@Accessors(chain = true)
public class LogContext {
    
    private String organizationId;
    private String userId;
    private String sessionId;
    private String requestId;
    private String correlationId;
    private String component;
    private String operation;
    private String resourceType;
    private String resourceId;
    private Long duration;
    private Integer statusCode;
    private Exception exception;
    private Map<String, Object> metadata;
    
    /**
     * Add metadata to the context
     */
    public LogContext addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Static factory methods for common scenarios
     */
    
    public static LogContext forRequest(String requestId, String operation) {
        return LogContext.builder()
            .requestId(requestId)
            .operation(operation)
            .build();
    }
    
    public static LogContext forUser(String userId, String organizationId) {
        return LogContext.builder()
            .userId(userId)
            .organizationId(organizationId)
            .build();
    }
    
    public static LogContext forResource(String resourceType, String resourceId) {
        return LogContext.builder()
            .resourceType(resourceType)
            .resourceId(resourceId)
            .build();
    }
    
    public static LogContext forError(Exception exception) {
        return LogContext.builder()
            .exception(exception)
            .build();
    }
    
    public static LogContext forPerformance(String operation, Long duration) {
        return LogContext.builder()
            .operation(operation)
            .duration(duration)
            .build();
    }
}