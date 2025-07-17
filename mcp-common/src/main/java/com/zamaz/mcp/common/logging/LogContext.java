package com.zamaz.mcp.common.logging;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Map;

/**
 * Context object for structured logging with enhanced tracing and lifecycle support
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
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String component;
    private String operation;
    private String resourceType;
    private String resourceId;
    private Long duration;
    private Integer statusCode;
    private Exception exception;
    private Map<String, Object> metadata;
    private String logLevel;
    private Instant startTime;
    private Instant endTime;
    private String serviceName;
    private String serviceVersion;
    private String environment;
    private String region;
    private String instanceId;
    private String nodeId;
    private String buildVersion;
    private String buildCommit;
    
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
            .correlationId(generateCorrelationId())
            .traceId(generateTraceId())
            .startTime(Instant.now())
            .build();
    }
    
    public static LogContext forUser(String userId, String organizationId) {
        return LogContext.builder()
            .userId(userId)
            .organizationId(organizationId)
            .correlationId(generateCorrelationId())
            .build();
    }
    
    public static LogContext forResource(String resourceType, String resourceId) {
        return LogContext.builder()
            .resourceType(resourceType)
            .resourceId(resourceId)
            .correlationId(generateCorrelationId())
            .build();
    }
    
    public static LogContext forError(Exception exception) {
        return LogContext.builder()
            .exception(exception)
            .correlationId(generateCorrelationId())
            .build();
    }
    
    public static LogContext forPerformance(String operation, Long duration) {
        return LogContext.builder()
            .operation(operation)
            .duration(duration)
            .correlationId(generateCorrelationId())
            .build();
    }
    
    public static LogContext forTrace(String traceId, String spanId, String parentSpanId) {
        return LogContext.builder()
            .traceId(traceId)
            .spanId(spanId)
            .parentSpanId(parentSpanId)
            .correlationId(generateCorrelationId())
            .build();
    }
    
    public static LogContext forService(String serviceName, String serviceVersion, String environment) {
        return LogContext.builder()
            .serviceName(serviceName)
            .serviceVersion(serviceVersion)
            .environment(environment)
            .correlationId(generateCorrelationId())
            .build();
    }
    
    /**
     * Generate a unique correlation ID
     */
    private static String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * Generate a unique trace ID
     */
    private static String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a unique span ID
     */
    private static String generateSpanId() {
        return Long.toHexString(System.nanoTime());
    }
    
    /**
     * Mark the end of an operation and calculate duration
     */
    public LogContext markEnd() {
        this.endTime = Instant.now();
        if (this.startTime != null) {
            this.duration = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
        return this;
    }
    
    /**
     * Create a child context for nested operations
     */
    public LogContext createChild(String operation) {
        return LogContext.builder()
            .correlationId(this.correlationId)
            .traceId(this.traceId)
            .spanId(generateSpanId())
            .parentSpanId(this.spanId)
            .organizationId(this.organizationId)
            .userId(this.userId)
            .sessionId(this.sessionId)
            .requestId(this.requestId)
            .operation(operation)
            .serviceName(this.serviceName)
            .serviceVersion(this.serviceVersion)
            .environment(this.environment)
            .region(this.region)
            .instanceId(this.instanceId)
            .nodeId(this.nodeId)
            .buildVersion(this.buildVersion)
            .buildCommit(this.buildCommit)
            .startTime(Instant.now())
            .build();
    }
}