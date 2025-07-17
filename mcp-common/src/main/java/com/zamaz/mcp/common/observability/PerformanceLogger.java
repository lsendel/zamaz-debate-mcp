package com.zamaz.mcp.common.observability;

import com.zamaz.mcp.common.logging.LogContext;
import com.zamaz.mcp.common.logging.StructuredLogger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Counter;
import io.opentelemetry.api.metrics.Histogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Performance logging component for capturing and analyzing performance metrics
 */
@Component
@Slf4j
public class PerformanceLogger {
    
    private final StructuredLogger structuredLogger;
    private final Meter meter;
    private final Counter operationCounter;
    private final Histogram operationDuration;
    private final Counter errorCounter;
    private final Histogram memoryUsage;
    private final Counter slowOperationCounter;
    
    // Track active operations
    private final ConcurrentMap<String, PerformanceContext> activeOperations = new ConcurrentHashMap<>();
    
    // Performance thresholds (in milliseconds)
    private static final long SLOW_OPERATION_THRESHOLD = 5000;
    private static final long VERY_SLOW_OPERATION_THRESHOLD = 10000;
    
    @Autowired
    public PerformanceLogger(StructuredLogger structuredLogger, OpenTelemetry openTelemetry) {
        this.structuredLogger = structuredLogger;
        this.meter = openTelemetry.getMeter("mcp.performance", "1.0.0");
        
        // Initialize metrics
        this.operationCounter = meter.counterBuilder("operation_total")
                .setDescription("Total number of operations")
                .build();
        
        this.operationDuration = meter.histogramBuilder("operation_duration_seconds")
                .setDescription("Duration of operations in seconds")
                .setUnit("s")
                .build();
        
        this.errorCounter = meter.counterBuilder("operation_errors_total")
                .setDescription("Total number of operation errors")
                .build();
        
        this.memoryUsage = meter.histogramBuilder("memory_usage_bytes")
                .setDescription("Memory usage in bytes")
                .setUnit("bytes")
                .build();
        
        this.slowOperationCounter = meter.counterBuilder("slow_operations_total")
                .setDescription("Total number of slow operations")
                .build();
    }
    
    /**
     * Start performance tracking for an operation
     */
    public String startOperation(String operationName, String component) {
        String operationId = java.util.UUID.randomUUID().toString();
        
        PerformanceContext context = new PerformanceContext(
            operationId,
            operationName,
            component,
            Instant.now(),
            getMemoryUsage(),
            Thread.currentThread().getName()
        );
        
        activeOperations.put(operationId, context);
        
        // Log operation start
        LogContext logContext = LogContext.builder()
                .operation(operationName)
                .component(component)
                .requestId(operationId)
                .startTime(context.getStartTime())
                .build();
        
        structuredLogger.debug(PerformanceLogger.class.getName(), 
            "Performance tracking started for operation: " + operationName, logContext);
        
        return operationId;
    }
    
    /**
     * End performance tracking for an operation
     */
    public void endOperation(String operationId) {
        endOperation(operationId, null, null);
    }
    
    /**
     * End performance tracking for an operation with result
     */
    public void endOperation(String operationId, String result, Throwable error) {
        PerformanceContext context = activeOperations.remove(operationId);
        
        if (context == null) {
            log.warn("Performance context not found for operation: {}", operationId);
            return;
        }
        
        Instant endTime = Instant.now();
        Duration duration = Duration.between(context.getStartTime(), endTime);
        long durationMs = duration.toMillis();
        
        // Update metrics
        operationCounter.add(1,
            io.opentelemetry.api.common.Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("operation"), context.getOperationName(),
                io.opentelemetry.api.common.AttributeKey.stringKey("component"), context.getComponent(),
                io.opentelemetry.api.common.AttributeKey.stringKey("result"), result != null ? result : "success"
            )
        );
        
        operationDuration.record(duration.toMillis() / 1000.0,
            io.opentelemetry.api.common.Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("operation"), context.getOperationName(),
                io.opentelemetry.api.common.AttributeKey.stringKey("component"), context.getComponent()
            )
        );
        
        // Record error if present
        if (error != null) {
            errorCounter.add(1,
                io.opentelemetry.api.common.Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("operation"), context.getOperationName(),
                    io.opentelemetry.api.common.AttributeKey.stringKey("component"), context.getComponent(),
                    io.opentelemetry.api.common.AttributeKey.stringKey("error_type"), error.getClass().getSimpleName()
                )
            );
        }
        
        // Check if operation is slow
        if (durationMs > SLOW_OPERATION_THRESHOLD) {
            slowOperationCounter.add(1,
                io.opentelemetry.api.common.Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("operation"), context.getOperationName(),
                    io.opentelemetry.api.common.AttributeKey.stringKey("component"), context.getComponent(),
                    io.opentelemetry.api.common.AttributeKey.stringKey("severity"), 
                    durationMs > VERY_SLOW_OPERATION_THRESHOLD ? "critical" : "warning"
                )
            );
        }
        
        // Record memory usage
        long currentMemory = getMemoryUsage();
        memoryUsage.record(currentMemory,
            io.opentelemetry.api.common.Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("operation"), context.getOperationName(),
                io.opentelemetry.api.common.AttributeKey.stringKey("component"), context.getComponent()
            )
        );
        
        // Log performance information
        logPerformanceResult(context, endTime, duration, currentMemory, result, error);
        
        // Add to current span if available
        addToCurrentSpan(context, duration, result, error);
    }
    
    /**
     * Log performance result
     */
    private void logPerformanceResult(PerformanceContext context, Instant endTime, Duration duration, 
                                    long currentMemory, String result, Throwable error) {
        
        LogContext logContext = LogContext.builder()
                .operation(context.getOperationName())
                .component(context.getComponent())
                .requestId(context.getOperationId())
                .startTime(context.getStartTime())
                .endTime(endTime)
                .duration(duration.toMillis())
                .exception(error)
                .build()
                .addMetadata("result", result)
                .addMetadata("startMemory", context.getStartMemory())
                .addMetadata("endMemory", currentMemory)
                .addMetadata("memoryDelta", currentMemory - context.getStartMemory())
                .addMetadata("threadName", context.getThreadName());
        
        String logLevel = determineLogLevel(duration.toMillis(), error);
        String message = String.format("Operation %s completed in %dms", 
            context.getOperationName(), duration.toMillis());
        
        switch (logLevel) {
            case "ERROR":
                structuredLogger.error(PerformanceLogger.class.getName(), message, logContext);
                break;
            case "WARN":
                structuredLogger.warn(PerformanceLogger.class.getName(), message, logContext);
                break;
            case "INFO":
                structuredLogger.info(PerformanceLogger.class.getName(), message, logContext);
                break;
            default:
                structuredLogger.debug(PerformanceLogger.class.getName(), message, logContext);
        }
    }
    
    /**
     * Add performance information to current span
     */
    private void addToCurrentSpan(PerformanceContext context, Duration duration, String result, Throwable error) {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            currentSpan.setAttribute("performance.operation", context.getOperationName());
            currentSpan.setAttribute("performance.component", context.getComponent());
            currentSpan.setAttribute("performance.duration_ms", duration.toMillis());
            currentSpan.setAttribute("performance.start_memory", context.getStartMemory());
            currentSpan.setAttribute("performance.thread", context.getThreadName());
            
            if (result != null) {
                currentSpan.setAttribute("performance.result", result);
            }
            
            if (error != null) {
                currentSpan.recordException(error);
            }
            
            // Add performance classification
            if (duration.toMillis() > VERY_SLOW_OPERATION_THRESHOLD) {
                currentSpan.setAttribute("performance.classification", "very_slow");
            } else if (duration.toMillis() > SLOW_OPERATION_THRESHOLD) {
                currentSpan.setAttribute("performance.classification", "slow");
            } else {
                currentSpan.setAttribute("performance.classification", "normal");
            }
        }
    }
    
    /**
     * Determine log level based on duration and error
     */
    private String determineLogLevel(long durationMs, Throwable error) {
        if (error != null) {
            return "ERROR";
        }
        
        if (durationMs > VERY_SLOW_OPERATION_THRESHOLD) {
            return "WARN";
        }
        
        if (durationMs > SLOW_OPERATION_THRESHOLD) {
            return "INFO";
        }
        
        return "DEBUG";
    }
    
    /**
     * Get current memory usage
     */
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Log system performance metrics
     */
    public void logSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        LogContext logContext = LogContext.builder()
                .operation("system_metrics")
                .component("performance")
                .build()
                .addMetadata("totalMemory", totalMemory)
                .addMetadata("freeMemory", freeMemory)
                .addMetadata("usedMemory", usedMemory)
                .addMetadata("maxMemory", maxMemory)
                .addMetadata("memoryUsagePercent", (double) usedMemory / maxMemory * 100)
                .addMetadata("availableProcessors", runtime.availableProcessors())
                .addMetadata("activeOperations", activeOperations.size());
        
        structuredLogger.info(PerformanceLogger.class.getName(), "System performance metrics", logContext);
        
        // Update memory metrics
        memoryUsage.record(usedMemory,
            io.opentelemetry.api.common.Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("metric_type"), "system"
            )
        );
    }
    
    /**
     * Get performance summary for active operations
     */
    public void logActiveOperationsSummary() {
        if (activeOperations.isEmpty()) {
            return;
        }
        
        Instant now = Instant.now();
        
        for (PerformanceContext context : activeOperations.values()) {
            Duration runningDuration = Duration.between(context.getStartTime(), now);
            
            if (runningDuration.toMillis() > SLOW_OPERATION_THRESHOLD) {
                LogContext logContext = LogContext.builder()
                        .operation(context.getOperationName())
                        .component(context.getComponent())
                        .requestId(context.getOperationId())
                        .startTime(context.getStartTime())
                        .duration(runningDuration.toMillis())
                        .build()
                        .addMetadata("status", "running")
                        .addMetadata("threadName", context.getThreadName());
                
                structuredLogger.warn(PerformanceLogger.class.getName(), 
                    "Long running operation detected: " + context.getOperationName(), logContext);
            }
        }
    }
    
    /**
     * Performance context for tracking operation lifecycle
     */
    private static class PerformanceContext {
        private final String operationId;
        private final String operationName;
        private final String component;
        private final Instant startTime;
        private final long startMemory;
        private final String threadName;
        
        public PerformanceContext(String operationId, String operationName, String component, 
                                Instant startTime, long startMemory, String threadName) {
            this.operationId = operationId;
            this.operationName = operationName;
            this.component = component;
            this.startTime = startTime;
            this.startMemory = startMemory;
            this.threadName = threadName;
        }
        
        public String getOperationId() { return operationId; }
        public String getOperationName() { return operationName; }
        public String getComponent() { return component; }
        public Instant getStartTime() { return startTime; }
        public long getStartMemory() { return startMemory; }
        public String getThreadName() { return threadName; }
    }
}