package com.zamaz.mcp.controller.monitoring;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects and exposes metrics for agentic flow operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Map<String, Counter> flowExecutionCounters = new HashMap<>();
    private final Map<String, Counter> flowErrorCounters = new HashMap<>();
    
    // Timers
    private final Map<String, Timer> flowExecutionTimers = new HashMap<>();
    
    // Gauges
    private final AtomicInteger activeFlowExecutions = new AtomicInteger(0);
    private final Map<String, AtomicInteger> activeFlowsByType = new HashMap<>();
    
    /**
     * Records the start of a flow execution.
     */
    public Timer.Sample startFlowExecution(String flowType, String organizationId) {
        activeFlowExecutions.incrementAndGet();
        activeFlowsByType.computeIfAbsent(flowType, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Register gauges if not already registered
        registerGaugesIfNeeded();
        
        return Timer.start(meterRegistry);
    }
    
    /**
     * Records the completion of a flow execution.
     */
    public void recordFlowCompletion(
            Timer.Sample sample,
            String flowType,
            String organizationId,
            boolean success,
            double confidence) {
        
        // Update counters
        String counterKey = String.format("agentic.flow.executions.%s", flowType.toLowerCase());
        flowExecutionCounters.computeIfAbsent(counterKey, k ->
            Counter.builder(k)
                .tag("flow_type", flowType)
                .tag("organization_id", organizationId)
                .description("Number of flow executions")
                .register(meterRegistry)
        ).increment();
        
        if (!success) {
            String errorKey = String.format("agentic.flow.errors.%s", flowType.toLowerCase());
            flowErrorCounters.computeIfAbsent(errorKey, k ->
                Counter.builder(k)
                    .tag("flow_type", flowType)
                    .tag("organization_id", organizationId)
                    .description("Number of flow execution errors")
                    .register(meterRegistry)
            ).increment();
        }
        
        // Update timer
        String timerKey = String.format("agentic.flow.duration.%s", flowType.toLowerCase());
        Timer timer = flowExecutionTimers.computeIfAbsent(timerKey, k ->
            Timer.builder(k)
                .tag("flow_type", flowType)
                .tag("organization_id", organizationId)
                .tag("success", String.valueOf(success))
                .description("Flow execution duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
        );
        
        sample.stop(timer);
        
        // Record confidence distribution
        if (confidence >= 0) {
            DistributionSummary.builder("agentic.flow.confidence")
                .tag("flow_type", flowType)
                .tag("organization_id", organizationId)
                .description("Flow execution confidence scores")
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry)
                .record(confidence);
        }
        
        // Update active executions
        activeFlowExecutions.decrementAndGet();
        activeFlowsByType.get(flowType).decrementAndGet();
    }
    
    /**
     * Records cache hit/miss metrics.
     */
    public void recordCacheMetrics(String cacheName, boolean hit) {
        Counter.builder("agentic.flow.cache")
            .tag("cache_name", cacheName)
            .tag("result", hit ? "hit" : "miss")
            .description("Cache hit/miss count")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Records queue metrics.
     */
    public void recordQueueMetrics(String queueName, int size, int consumers) {
        Gauge.builder("agentic.flow.queue.size", size, Integer::doubleValue)
            .tag("queue_name", queueName)
            .description("Current queue size")
            .register(meterRegistry);
        
        Gauge.builder("agentic.flow.queue.consumers", consumers, Integer::doubleValue)
            .tag("queue_name", queueName)
            .description("Active queue consumers")
            .register(meterRegistry);
    }
    
    /**
     * Records resource usage metrics.
     */
    public void recordResourceUsage(String resourceType, double usage) {
        Gauge.builder("agentic.flow.resource.usage", usage, Double::doubleValue)
            .tag("resource_type", resourceType)
            .description("Resource usage percentage")
            .register(meterRegistry);
    }
    
    /**
     * Records LLM token usage.
     */
    public void recordTokenUsage(String flowType, int promptTokens, int completionTokens) {
        Counter.builder("agentic.flow.tokens.prompt")
            .tag("flow_type", flowType)
            .description("Prompt token usage")
            .register(meterRegistry)
            .increment(promptTokens);
        
        Counter.builder("agentic.flow.tokens.completion")
            .tag("flow_type", flowType)
            .description("Completion token usage")
            .register(meterRegistry)
            .increment(completionTokens);
    }
    
    /**
     * Records external service call metrics.
     */
    public void recordExternalServiceCall(String service, boolean success, long durationMs) {
        Counter.builder("agentic.flow.external.calls")
            .tag("service", service)
            .tag("success", String.valueOf(success))
            .description("External service call count")
            .register(meterRegistry)
            .increment();
        
        Timer.builder("agentic.flow.external.duration")
            .tag("service", service)
            .tag("success", String.valueOf(success))
            .description("External service call duration")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    private void registerGaugesIfNeeded() {
        // Register active executions gauge
        if (!meterRegistry.getMeters().stream()
                .anyMatch(m -> m.getId().getName().equals("agentic.flow.active.total"))) {
            
            Gauge.builder("agentic.flow.active.total", activeFlowExecutions, AtomicInteger::get)
                .description("Total active flow executions")
                .register(meterRegistry);
        }
        
        // Register per-type active executions
        activeFlowsByType.forEach((flowType, count) -> {
            String gaugeName = String.format("agentic.flow.active.%s", flowType.toLowerCase());
            if (!meterRegistry.getMeters().stream()
                    .anyMatch(m -> m.getId().getName().equals(gaugeName))) {
                
                Gauge.builder(gaugeName, count, AtomicInteger::get)
                    .tag("flow_type", flowType)
                    .description("Active executions by flow type")
                    .register(meterRegistry);
            }
        });
    }
}