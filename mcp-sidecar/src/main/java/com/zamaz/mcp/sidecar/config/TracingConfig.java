package com.zamaz.mcp.sidecar.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed Tracing Configuration for MCP Sidecar
 * 
 * Provides comprehensive tracing capabilities:
 * - Request tracing across services
 * - Performance monitoring
 * - Error tracking
 * - Custom span creation
 * - Trace correlation
 * - Metrics integration
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TracingConfig {

    @Value("${app.tracing.enabled:true}")
    private boolean tracingEnabled;

    @Value("${app.tracing.sampling-rate:0.1}")
    private double samplingRate;

    @Value("${app.tracing.service-name:mcp-sidecar}")
    private String serviceName;

    /**
     * Custom trace context manager
     */
    @Bean
    public TraceContextManager traceContextManager() {
        return new TraceContextManager();
    }

    /**
     * Trace context manager for managing trace information
     */
    public static class TraceContextManager {
        private final Map<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
        private final Map<String, TraceMetrics> traceMetrics = new ConcurrentHashMap<>();

        /**
         * Start a new trace
         */
        public TraceContext startTrace(String operationName, Map<String, String> tags) {
            String traceId = UUID.randomUUID().toString();
            String spanId = UUID.randomUUID().toString();
            
            TraceContext context = new TraceContext(traceId, spanId, operationName, tags);
            activeTraces.put(traceId, context);
            
            log.debug("Started trace: traceId={}, operation={}", traceId, operationName);
            return context;
        }

        /**
         * Create child span
         */
        public TraceContext createChildSpan(String parentTraceId, String operationName, Map<String, String> tags) {
            TraceContext parent = activeTraces.get(parentTraceId);
            if (parent == null) {
                log.warn("Parent trace not found: {}", parentTraceId);
                return startTrace(operationName, tags);
            }

            String spanId = UUID.randomUUID().toString();
            TraceContext childContext = new TraceContext(parentTraceId, spanId, operationName, tags);
            childContext.setParentSpanId(parent.getSpanId());
            
            log.debug("Created child span: traceId={}, parentSpanId={}, operation={}", 
                    parentTraceId, parent.getSpanId(), operationName);
            
            return childContext;
        }

        /**
         * Finish trace
         */
        public void finishTrace(String traceId, TraceResult result) {
            TraceContext context = activeTraces.remove(traceId);
            if (context != null) {
                context.finish(result);
                recordTraceMetrics(context);
                
                log.debug("Finished trace: traceId={}, duration={}ms, result={}", 
                        traceId, context.getDuration().toMillis(), result);
            }
        }

        /**
         * Get active trace
         */
        public TraceContext getTrace(String traceId) {
            return activeTraces.get(traceId);
        }

        /**
         * Get trace metrics
         */
        public Map<String, TraceMetrics> getTraceMetrics() {
            return new HashMap<>(traceMetrics);
        }

        /**
         * Record trace metrics
         */
        private void recordTraceMetrics(TraceContext context) {
            String operation = context.getOperationName();
            TraceMetrics metrics = traceMetrics.computeIfAbsent(operation, k -> new TraceMetrics());
            
            metrics.recordTrace(context.getDuration(), context.getResult());
        }
    }

    /**
     * Trace context
     */
    public static class TraceContext {
        private final String traceId;
        private final String spanId;
        private String parentSpanId;
        private final String operationName;
        private final Map<String, String> tags;
        private final Instant startTime;
        private Instant endTime;
        private TraceResult result;
        private String errorMessage;
        private final Map<String, Object> attributes;

        public TraceContext(String traceId, String spanId, String operationName, Map<String, String> tags) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.operationName = operationName;
            this.tags = tags != null ? new HashMap<>(tags) : new HashMap<>();
            this.startTime = Instant.now();
            this.attributes = new HashMap<>();
        }

        public void finish(TraceResult result) {
            this.endTime = Instant.now();
            this.result = result;
        }

        public void setError(String errorMessage) {
            this.errorMessage = errorMessage;
            this.result = TraceResult.ERROR;
        }

        public void addAttribute(String key, Object value) {
            this.attributes.put(key, value);
        }

        public void addTag(String key, String value) {
            this.tags.put(key, value);
        }

        // Getters
        public String getTraceId() { return traceId; }
        public String getSpanId() { return spanId; }
        public String getParentSpanId() { return parentSpanId; }
        public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }
        public String getOperationName() { return operationName; }
        public Map<String, String> getTags() { return tags; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public TraceResult getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Object> getAttributes() { return attributes; }
        
        public Duration getDuration() {
            Instant end = endTime != null ? endTime : Instant.now();
            return Duration.between(startTime, end);
        }
    }

    /**
     * Trace result enumeration
     */
    public enum TraceResult {
        SUCCESS, ERROR, TIMEOUT, CANCELLED
    }

    /**
     * Trace metrics
     */
    public static class TraceMetrics {
        private long totalCount = 0;
        private long successCount = 0;
        private long errorCount = 0;
        private long timeoutCount = 0;
        private long totalDuration = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;

        public synchronized void recordTrace(Duration duration, TraceResult result) {
            totalCount++;
            long durationMs = duration.toMillis();
            totalDuration += durationMs;
            
            if (durationMs < minDuration) minDuration = durationMs;
            if (durationMs > maxDuration) maxDuration = durationMs;
            
            switch (result) {
                case SUCCESS -> successCount++;
                case ERROR -> errorCount++;
                case TIMEOUT -> timeoutCount++;
            }
        }

        public long getTotalCount() { return totalCount; }
        public long getSuccessCount() { return successCount; }
        public long getErrorCount() { return errorCount; }
        public long getTimeoutCount() { return timeoutCount; }
        public double getSuccessRate() { 
            return totalCount > 0 ? (double) successCount / totalCount : 0.0;
        }
        public double getErrorRate() { 
            return totalCount > 0 ? (double) errorCount / totalCount : 0.0;
        }
        public double getAverageDuration() { 
            return totalCount > 0 ? (double) totalDuration / totalCount : 0.0;
        }
        public long getMinDuration() { return minDuration == Long.MAX_VALUE ? 0 : minDuration; }
        public long getMaxDuration() { return maxDuration; }
    }

    /**
     * Tracing service for business logic
     */
    @Bean
    public TracingService tracingService(TraceContextManager traceContextManager) {
        return new TracingService(traceContextManager);
    }

    /**
     * Tracing service implementation
     */
    public static class TracingService {
        private final TraceContextManager traceContextManager;

        public TracingService(TraceContextManager traceContextManager) {
            this.traceContextManager = traceContextManager;
        }

        /**
         * Trace authentication operation
         */
        public Mono<String> traceAuthentication(String userId, Mono<String> operation) {
            Map<String, String> tags = Map.of(
                    "operation", "authentication",
                    "user_id", userId,
                    "service", "mcp-sidecar"
            );
            
            TraceContext context = traceContextManager.startTrace("authentication", tags);
            
            return operation
                    .doOnNext(result -> {
                        context.addAttribute("result", "success");
                        context.addAttribute("user_id", userId);
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.SUCCESS);
                    })
                    .doOnError(error -> {
                        context.setError(error.getMessage());
                        context.addAttribute("error_type", error.getClass().getSimpleName());
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.ERROR);
                    });
        }

        /**
         * Trace AI service call
         */
        public Mono<String> traceAIServiceCall(String model, String prompt, Mono<String> operation) {
            Map<String, String> tags = Map.of(
                    "operation", "ai_service_call",
                    "model", model,
                    "service", "mcp-sidecar"
            );
            
            TraceContext context = traceContextManager.startTrace("ai_service_call", tags);
            context.addAttribute("model", model);
            context.addAttribute("prompt_length", prompt.length());
            
            return operation
                    .doOnNext(result -> {
                        context.addAttribute("result", "success");
                        context.addAttribute("response_length", result.length());
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.SUCCESS);
                    })
                    .doOnError(error -> {
                        context.setError(error.getMessage());
                        context.addAttribute("error_type", error.getClass().getSimpleName());
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.ERROR);
                    });
        }

        /**
         * Trace service routing
         */
        public Mono<String> traceServiceRouting(String targetService, String path, Mono<String> operation) {
            Map<String, String> tags = Map.of(
                    "operation", "service_routing",
                    "target_service", targetService,
                    "path", path,
                    "service", "mcp-sidecar"
            );
            
            TraceContext context = traceContextManager.startTrace("service_routing", tags);
            context.addAttribute("target_service", targetService);
            context.addAttribute("path", path);
            
            return operation
                    .doOnNext(result -> {
                        context.addAttribute("result", "success");
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.SUCCESS);
                    })
                    .doOnError(error -> {
                        context.setError(error.getMessage());
                        context.addAttribute("error_type", error.getClass().getSimpleName());
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.ERROR);
                    });
        }

        /**
         * Trace RBAC operation
         */
        public Mono<Boolean> traceRBACOperation(String userId, String organizationId, String permission, 
                                               Mono<Boolean> operation) {
            Map<String, String> tags = Map.of(
                    "operation", "rbac_check",
                    "user_id", userId,
                    "organization_id", organizationId,
                    "permission", permission,
                    "service", "mcp-sidecar"
            );
            
            TraceContext context = traceContextManager.startTrace("rbac_check", tags);
            context.addAttribute("user_id", userId);
            context.addAttribute("organization_id", organizationId);
            context.addAttribute("permission", permission);
            
            return operation
                    .doOnNext(result -> {
                        context.addAttribute("result", result ? "allowed" : "denied");
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.SUCCESS);
                    })
                    .doOnError(error -> {
                        context.setError(error.getMessage());
                        context.addAttribute("error_type", error.getClass().getSimpleName());
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.ERROR);
                    });
        }

        /**
         * Trace cache operation
         */
        public Mono<String> traceCacheOperation(String operation, String key, Mono<String> cacheOperation) {
            Map<String, String> tags = Map.of(
                    "operation", "cache_" + operation,
                    "cache_key", key,
                    "service", "mcp-sidecar"
            );
            
            TraceContext context = traceContextManager.startTrace("cache_" + operation, tags);
            context.addAttribute("cache_key", key);
            
            return cacheOperation
                    .doOnNext(result -> {
                        context.addAttribute("result", result != null ? "hit" : "miss");
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.SUCCESS);
                    })
                    .doOnError(error -> {
                        context.setError(error.getMessage());
                        context.addAttribute("error_type", error.getClass().getSimpleName());
                        traceContextManager.finishTrace(context.getTraceId(), TraceResult.ERROR);
                    });
        }

        /**
         * Get tracing statistics
         */
        public Map<String, Object> getTracingStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("enabled", true);
            stats.put("active_traces", traceContextManager.activeTraces.size());
            stats.put("metrics", traceContextManager.getTraceMetrics());
            return stats;
        }
    }
}