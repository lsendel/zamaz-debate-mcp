package com.zamaz.workflow.telemetry.service;

import com.zamaz.workflow.application.service.WorkflowApplicationService;
import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.entity.WorkflowNode;
import com.zamaz.workflow.telemetry.model.TelemetryData;
import com.zamaz.workflow.telemetry.model.ThresholdRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTriggerService {
    
    private final WorkflowApplicationService workflowService;
    private final TelemetryThresholdMonitor thresholdMonitor;
    private final WorkflowExecutionService executionService;
    
    // Cache for active workflow triggers
    private final Map<String, List<TelemetryTrigger>> activeTriggers = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastTriggerTime = new ConcurrentHashMap<>();
    
    // Cooldown period to prevent trigger spam
    private static final Duration TRIGGER_COOLDOWN = Duration.ofMinutes(1);
    
    public List<String> checkTriggers(TelemetryData data) {
        List<String> triggeredWorkflows = new ArrayList<>();
        
        // Get triggers for this device
        List<TelemetryTrigger> triggers = getTelemetryTriggers(data.getDeviceId());
        
        for (TelemetryTrigger trigger : triggers) {
            if (evaluateTrigger(trigger, data)) {
                String workflowId = trigger.getWorkflowId();
                
                // Check cooldown
                if (isInCooldown(workflowId)) {
                    log.debug("Workflow {} is in cooldown, skipping trigger", workflowId);
                    continue;
                }
                
                triggeredWorkflows.add(workflowId);
                lastTriggerTime.put(workflowId, Instant.now());
                
                // Execute workflow asynchronously
                executeWorkflowAsync(workflowId, data);
                
                log.info("Triggered workflow {} for device {} due to condition: {}", 
                    workflowId, data.getDeviceId(), trigger.getCondition());
            }
        }
        
        return triggeredWorkflows;
    }
    
    private List<TelemetryTrigger> getTelemetryTriggers(String deviceId) {
        // Get from cache or load from database
        return activeTriggers.computeIfAbsent(deviceId, this::loadTriggersForDevice);
    }
    
    private List<TelemetryTrigger> loadTriggersForDevice(String deviceId) {
        // Load all workflows that have telemetry triggers
        List<Workflow> workflows = workflowService.getWorkflowsWithTelemetryTriggers();
        
        return workflows.stream()
            .flatMap(workflow -> extractTelemetryTriggers(workflow, deviceId).stream())
            .collect(Collectors.toList());
    }
    
    private List<TelemetryTrigger> extractTelemetryTriggers(Workflow workflow, String deviceId) {
        List<TelemetryTrigger> triggers = new ArrayList<>();
        
        // Find start nodes with telemetry triggers
        workflow.getNodes().stream()
            .filter(node -> "start".equals(node.getType()) && 
                          hasTelemetryTrigger(node))
            .forEach(node -> {
                TelemetryTrigger trigger = createTriggerFromNode(workflow.getId(), node, deviceId);
                if (trigger != null) {
                    triggers.add(trigger);
                }
            });
        
        // Find decision nodes with telemetry conditions
        workflow.getNodes().stream()
            .filter(node -> "decision".equals(node.getType()) && 
                          hasTelemetryCondition(node))
            .forEach(node -> {
                TelemetryTrigger trigger = createTriggerFromDecisionNode(workflow.getId(), node, deviceId);
                if (trigger != null) {
                    triggers.add(trigger);
                }
            });
        
        return triggers;
    }
    
    private boolean hasTelemetryTrigger(WorkflowNode node) {
        Object config = node.getData().get("configuration");
        if (config instanceof Map) {
            Map<?, ?> configMap = (Map<?, ?>) config;
            return "telemetry".equals(configMap.get("trigger")) ||
                   "event".equals(configMap.get("trigger"));
        }
        return false;
    }
    
    private boolean hasTelemetryCondition(WorkflowNode node) {
        Object config = node.getData().get("configuration");
        if (config instanceof Map) {
            Map<?, ?> configMap = (Map<?, ?>) config;
            return "telemetry".equals(configMap.get("decisionType"));
        }
        return false;
    }
    
    private TelemetryTrigger createTriggerFromNode(String workflowId, WorkflowNode node, String deviceId) {
        Object config = node.getData().get("configuration");
        if (!(config instanceof Map)) return null;
        
        Map<?, ?> configMap = (Map<?, ?>) config;
        String eventType = (String) configMap.get("eventType");
        
        if (eventType == null) return null;
        
        // Parse event type (e.g., "sensor.temperature.threshold")
        String[] parts = eventType.split("\\.");
        if (parts.length < 3) return null;
        
        String metric = parts[1];
        String condition = parts[2];
        
        return TelemetryTrigger.builder()
            .workflowId(workflowId)
            .nodeId(node.getId())
            .deviceId(deviceId)
            .metric(metric)
            .condition(condition)
            .threshold(getThresholdFromConfig(configMap))
            .operator(getOperatorFromCondition(condition))
            .build();
    }
    
    private TelemetryTrigger createTriggerFromDecisionNode(String workflowId, WorkflowNode node, String deviceId) {
        Object config = node.getData().get("configuration");
        if (!(config instanceof Map)) return null;
        
        Map<?, ?> configMap = (Map<?, ?>) config;
        Object conditionObj = configMap.get("condition");
        
        if (!(conditionObj instanceof Map)) return null;
        
        Map<?, ?> condition = (Map<?, ?>) conditionObj;
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> rules = (List<Map<?, ?>>) condition.get("rules");
        
        if (rules == null || rules.isEmpty()) return null;
        
        Map<?, ?> rule = rules.get(0); // Use first rule for simplicity
        
        return TelemetryTrigger.builder()
            .workflowId(workflowId)
            .nodeId(node.getId())
            .deviceId(deviceId)
            .metric((String) rule.get("field"))
            .operator((String) rule.get("operator"))
            .threshold(Double.valueOf(rule.get("value").toString()))
            .condition("threshold")
            .build();
    }
    
    private Double getThresholdFromConfig(Map<?, ?> config) {
        Object threshold = config.get("threshold");
        if (threshold instanceof Number) {
            return ((Number) threshold).doubleValue();
        }
        return 0.0;
    }
    
    private String getOperatorFromCondition(String condition) {
        switch (condition.toLowerCase()) {
            case "high":
            case "above":
                return ">";
            case "low":
            case "below":
                return "<";
            case "equals":
                return "=";
            default:
                return ">";
        }
    }
    
    private boolean evaluateTrigger(TelemetryTrigger trigger, TelemetryData data) {
        Object metricValue = data.getMetrics().get(trigger.getMetric());
        if (!(metricValue instanceof Number)) {
            return false;
        }
        
        double value = ((Number) metricValue).doubleValue();
        double threshold = trigger.getThreshold();
        
        switch (trigger.getOperator()) {
            case ">":
                return value > threshold;
            case "<":
                return value < threshold;
            case ">=":
                return value >= threshold;
            case "<=":
                return value <= threshold;
            case "=":
            case "==":
                return Math.abs(value - threshold) < 0.001;
            case "!=":
                return Math.abs(value - threshold) >= 0.001;
            default:
                return false;
        }
    }
    
    private boolean isInCooldown(String workflowId) {
        Instant lastTrigger = lastTriggerTime.get(workflowId);
        if (lastTrigger == null) return false;
        
        return Duration.between(lastTrigger, Instant.now()).compareTo(TRIGGER_COOLDOWN) < 0;
    }
    
    private void executeWorkflowAsync(String workflowId, TelemetryData data) {
        Mono.fromRunnable(() -> {
            try {
                Map<String, Object> context = new HashMap<>();
                context.put("triggerData", data);
                context.put("deviceId", data.getDeviceId());
                context.put("timestamp", data.getTimestamp());
                context.put("metrics", data.getMetrics());
                
                executionService.executeWorkflow(workflowId, context);
                
                log.info("Successfully executed workflow {} for device {}", 
                    workflowId, data.getDeviceId());
            } catch (Exception e) {
                log.error("Failed to execute workflow {} for device {}: {}", 
                    workflowId, data.getDeviceId(), e.getMessage());
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .subscribe();
    }
    
    // Refresh triggers cache
    public void refreshTriggers() {
        log.info("Refreshing telemetry triggers cache");
        activeTriggers.clear();
    }
    
    // Add new trigger manually
    public void addTrigger(TelemetryTrigger trigger) {
        activeTriggers.computeIfAbsent(trigger.getDeviceId(), k -> new ArrayList<>())
            .add(trigger);
        log.info("Added telemetry trigger for workflow {} device {}", 
            trigger.getWorkflowId(), trigger.getDeviceId());
    }
    
    // Get trigger statistics
    public TriggerStats getStats() {
        int totalTriggers = activeTriggers.values().stream()
            .mapToInt(List::size)
            .sum();
        
        return TriggerStats.builder()
            .totalTriggers(totalTriggers)
            .deviceCount(activeTriggers.size())
            .activeWorkflows((int) lastTriggerTime.size())
            .lastRefresh(Instant.now())
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TelemetryTrigger {
        private final String workflowId;
        private final String nodeId;
        private final String deviceId;
        private final String metric;
        private final String operator;
        private final Double threshold;
        private final String condition;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TriggerStats {
        private final int totalTriggers;
        private final int deviceCount;
        private final int activeWorkflows;
        private final Instant lastRefresh;
    }
}

// Supporting service for workflow execution
@Service
@RequiredArgsConstructor
@Slf4j
class WorkflowExecutionService {
    
    private final WorkflowApplicationService workflowService;
    
    public void executeWorkflow(String workflowId, Map<String, Object> context) {
        try {
            // Start workflow execution
            String executionId = workflowService.startExecution(workflowId, context);
            
            log.info("Started workflow execution {} for workflow {}", executionId, workflowId);
            
            // Monitor execution progress
            monitorExecution(executionId);
            
        } catch (Exception e) {
            log.error("Failed to execute workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Workflow execution failed", e);
        }
    }
    
    private void monitorExecution(String executionId) {
        // This would integrate with the workflow execution engine
        // For now, we'll just log the execution
        log.debug("Monitoring workflow execution: {}", executionId);
    }
}

// Threshold monitoring service
@Service
@Slf4j
class TelemetryThresholdMonitor {
    
    private final Map<String, ThresholdRule> thresholdRules = new ConcurrentHashMap<>();
    
    public void addThresholdRule(ThresholdRule rule) {
        thresholdRules.put(rule.getId(), rule);
        log.info("Added threshold rule: {}", rule);
    }
    
    public void removeThresholdRule(String ruleId) {
        thresholdRules.remove(ruleId);
        log.info("Removed threshold rule: {}", ruleId);
    }
    
    public List<ThresholdRule> checkThresholds(TelemetryData data) {
        return thresholdRules.values().stream()
            .filter(rule -> rule.matches(data))
            .filter(rule -> rule.isViolated(data))
            .collect(Collectors.toList());
    }
    
    public int getRuleCount() {
        return thresholdRules.size();
    }
}