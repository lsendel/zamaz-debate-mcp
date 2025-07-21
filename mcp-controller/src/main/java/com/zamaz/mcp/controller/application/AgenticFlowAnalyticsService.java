package com.zamaz.mcp.controller.application;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.controller.domain.analytics.*;
import com.zamaz.mcp.controller.port.out.AgenticFlowAnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Application service for collecting and managing agentic flow analytics.
 * Tracks execution metrics, performance data, and usage patterns.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowAnalyticsService {
    private final AgenticFlowAnalyticsRepository analyticsRepository;
    
    /**
     * Records the execution of an agentic flow.
     */
    public CompletableFuture<AgenticFlowExecution> recordExecution(
            UUID flowId,
            UUID debateId,
            UUID organizationId,
            AgenticFlowResult result,
            Duration executionTime) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                AgenticFlowExecution execution = AgenticFlowExecution.builder()
                    .id(UUID.randomUUID())
                    .flowId(flowId)
                    .debateId(debateId)
                    .organizationId(organizationId)
                    .flowType(result.getFlowType())
                    .executionTime(executionTime)
                    .status(result.getStatus())
                    .confidence(result.getConfidence())
                    .timestamp(LocalDateTime.now())
                    .metadata(extractMetadata(result))
                    .build();
                
                return analyticsRepository.save(execution);
            } catch (Exception e) {
                log.error("Failed to record agentic flow execution", e);
                throw new RuntimeException("Failed to record execution", e);
            }
        });
    }
    
    /**
     * Records performance metrics for an agentic flow execution.
     */
    public CompletableFuture<Void> recordPerformanceMetrics(
            UUID executionId,
            Map<String, Object> metrics) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                AgenticFlowPerformanceMetrics performanceMetrics = AgenticFlowPerformanceMetrics.builder()
                    .id(UUID.randomUUID())
                    .executionId(executionId)
                    .llmResponseTime(extractLong(metrics, "llmResponseTime"))
                    .toolCallTime(extractLong(metrics, "toolCallTime"))
                    .totalTokens(extractInt(metrics, "totalTokens"))
                    .promptTokens(extractInt(metrics, "promptTokens"))
                    .completionTokens(extractInt(metrics, "completionTokens"))
                    .memoryUsage(extractLong(metrics, "memoryUsage"))
                    .timestamp(LocalDateTime.now())
                    .build();
                
                analyticsRepository.savePerformanceMetrics(performanceMetrics);
            } catch (Exception e) {
                log.error("Failed to record performance metrics", e);
            }
        });
    }
    
    /**
     * Gets aggregated analytics for a specific flow type.
     */
    public CompletableFuture<AgenticFlowAnalyticsSummary> getFlowTypeAnalytics(
            UUID organizationId,
            AgenticFlowType flowType,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<AgenticFlowExecution> executions = analyticsRepository
                .findByOrganizationAndFlowTypeAndDateRange(
                    organizationId, flowType, startDate, endDate);
            
            return aggregateAnalytics(executions, flowType);
        });
    }
    
    /**
     * Gets analytics for a specific debate.
     */
    public CompletableFuture<DebateAgenticFlowAnalytics> getDebateAnalytics(
            UUID debateId) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<AgenticFlowExecution> executions = analyticsRepository
                .findByDebateId(debateId);
            
            Map<AgenticFlowType, List<AgenticFlowExecution>> executionsByType = 
                executions.stream()
                    .collect(Collectors.groupingBy(AgenticFlowExecution::getFlowType));
            
            Map<AgenticFlowType, AgenticFlowAnalyticsSummary> summaries = new HashMap<>();
            executionsByType.forEach((type, typeExecutions) -> {
                summaries.put(type, aggregateAnalytics(typeExecutions, type));
            });
            
            return DebateAgenticFlowAnalytics.builder()
                .debateId(debateId)
                .totalExecutions(executions.size())
                .flowTypeSummaries(summaries)
                .averageConfidence(calculateAverageConfidence(executions))
                .successRate(calculateSuccessRate(executions))
                .timestamp(LocalDateTime.now())
                .build();
        });
    }
    
    /**
     * Gets trending flow types based on usage and performance.
     */
    public CompletableFuture<List<TrendingFlowType>> getTrendingFlowTypes(
            UUID organizationId,
            int limit) {
        
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            List<AgenticFlowExecution> recentExecutions = analyticsRepository
                .findByOrganizationAndDateRange(organizationId, thirtyDaysAgo, LocalDateTime.now());
            
            Map<AgenticFlowType, FlowTypeStats> stats = calculateFlowTypeStats(recentExecutions);
            
            return stats.entrySet().stream()
                .map(entry -> TrendingFlowType.builder()
                    .flowType(entry.getKey())
                    .usageCount(entry.getValue().getUsageCount())
                    .averageConfidence(entry.getValue().getAverageConfidence())
                    .successRate(entry.getValue().getSuccessRate())
                    .averageExecutionTime(entry.getValue().getAverageExecutionTime())
                    .trendScore(calculateTrendScore(entry.getValue()))
                    .build())
                .sorted(Comparator.comparing(TrendingFlowType::getTrendScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        });
    }
    
    /**
     * Records when an agentic flow result leads to a response improvement.
     */
    public CompletableFuture<Void> recordResponseImprovement(
            UUID executionId,
            double improvementScore,
            String improvementReason) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                analyticsRepository.updateResponseImprovement(
                    executionId, improvementScore, improvementReason);
            } catch (Exception e) {
                log.error("Failed to record response improvement", e);
            }
        });
    }
    
    /**
     * Gets performance comparison between different flow types.
     */
    public CompletableFuture<FlowTypeComparison> compareFlowTypes(
            UUID organizationId,
            Set<AgenticFlowType> flowTypes,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<AgenticFlowType, List<AgenticFlowExecution>> executionsByType = new HashMap<>();
            
            for (AgenticFlowType flowType : flowTypes) {
                List<AgenticFlowExecution> executions = analyticsRepository
                    .findByOrganizationAndFlowTypeAndDateRange(
                        organizationId, flowType, startDate, endDate);
                executionsByType.put(flowType, executions);
            }
            
            return FlowTypeComparison.builder()
                .flowTypes(flowTypes)
                .comparisons(buildComparisons(executionsByType))
                .recommendation(generateRecommendation(executionsByType))
                .timestamp(LocalDateTime.now())
                .build();
        });
    }
    
    // Helper methods
    
    private Map<String, Object> extractMetadata(AgenticFlowResult result) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (result.getIterations() != null) {
            metadata.put("iterationCount", result.getIterations().size());
        }
        if (result.getToolCalls() != null) {
            metadata.put("toolCallCount", result.getToolCalls().size());
        }
        if (result.getDocuments() != null) {
            metadata.put("documentCount", result.getDocuments().size());
            long selectedDocs = result.getDocuments().stream()
                .filter(AgenticFlowResult.Document::isSelected)
                .count();
            metadata.put("selectedDocumentCount", selectedDocs);
        }
        if (result.getViolations() != null) {
            metadata.put("violationCount", result.getViolations().size());
        }
        
        return metadata;
    }
    
    private AgenticFlowAnalyticsSummary aggregateAnalytics(
            List<AgenticFlowExecution> executions,
            AgenticFlowType flowType) {
        
        if (executions.isEmpty()) {
            return AgenticFlowAnalyticsSummary.empty(flowType);
        }
        
        double avgConfidence = calculateAverageConfidence(executions);
        double successRate = calculateSuccessRate(executions);
        Duration avgExecutionTime = calculateAverageExecutionTime(executions);
        
        Map<String, Object> aggregatedMetrics = new HashMap<>();
        aggregatedMetrics.put("totalExecutions", executions.size());
        aggregatedMetrics.put("averageConfidence", avgConfidence);
        aggregatedMetrics.put("successRate", successRate);
        aggregatedMetrics.put("averageExecutionTimeMs", avgExecutionTime.toMillis());
        
        return AgenticFlowAnalyticsSummary.builder()
            .flowType(flowType)
            .executionCount(executions.size())
            .averageConfidence(avgConfidence)
            .successRate(successRate)
            .averageExecutionTime(avgExecutionTime)
            .metrics(aggregatedMetrics)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private double calculateAverageConfidence(List<AgenticFlowExecution> executions) {
        return executions.stream()
            .map(AgenticFlowExecution::getConfidence)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    private double calculateSuccessRate(List<AgenticFlowExecution> executions) {
        long successCount = executions.stream()
            .filter(e -> AgenticFlowStatus.SUCCESS.equals(e.getStatus()))
            .count();
        return executions.isEmpty() ? 0.0 : (double) successCount / executions.size();
    }
    
    private Duration calculateAverageExecutionTime(List<AgenticFlowExecution> executions) {
        double avgMillis = executions.stream()
            .map(AgenticFlowExecution::getExecutionTime)
            .mapToLong(Duration::toMillis)
            .average()
            .orElse(0.0);
        return Duration.ofMillis(Math.round(avgMillis));
    }
    
    private Map<AgenticFlowType, FlowTypeStats> calculateFlowTypeStats(
            List<AgenticFlowExecution> executions) {
        
        return executions.stream()
            .collect(Collectors.groupingBy(
                AgenticFlowExecution::getFlowType,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> FlowTypeStats.builder()
                        .usageCount(list.size())
                        .averageConfidence(calculateAverageConfidence(list))
                        .successRate(calculateSuccessRate(list))
                        .averageExecutionTime(calculateAverageExecutionTime(list))
                        .build()
                )));
    }
    
    private double calculateTrendScore(FlowTypeStats stats) {
        // Weighted scoring: usage (40%), confidence (30%), success rate (30%)
        double usageScore = Math.min(stats.getUsageCount() / 100.0, 1.0) * 0.4;
        double confidenceScore = (stats.getAverageConfidence() / 100.0) * 0.3;
        double successScore = stats.getSuccessRate() * 0.3;
        
        return usageScore + confidenceScore + successScore;
    }
    
    private Map<String, Object> buildComparisons(
            Map<AgenticFlowType, List<AgenticFlowExecution>> executionsByType) {
        
        Map<String, Object> comparisons = new HashMap<>();
        
        // Find best performing flow type by confidence
        AgenticFlowType bestByConfidence = executionsByType.entrySet().stream()
            .max(Comparator.comparing(e -> calculateAverageConfidence(e.getValue())))
            .map(Map.Entry::getKey)
            .orElse(null);
        comparisons.put("bestByConfidence", bestByConfidence);
        
        // Find fastest flow type
        AgenticFlowType fastest = executionsByType.entrySet().stream()
            .min(Comparator.comparing(e -> calculateAverageExecutionTime(e.getValue())))
            .map(Map.Entry::getKey)
            .orElse(null);
        comparisons.put("fastestFlowType", fastest);
        
        // Find most reliable (highest success rate)
        AgenticFlowType mostReliable = executionsByType.entrySet().stream()
            .max(Comparator.comparing(e -> calculateSuccessRate(e.getValue())))
            .map(Map.Entry::getKey)
            .orElse(null);
        comparisons.put("mostReliableFlowType", mostReliable);
        
        return comparisons;
    }
    
    private String generateRecommendation(
            Map<AgenticFlowType, List<AgenticFlowExecution>> executionsByType) {
        
        // Simple recommendation logic based on performance metrics
        Map.Entry<AgenticFlowType, List<AgenticFlowExecution>> bestOverall = 
            executionsByType.entrySet().stream()
                .max(Comparator.comparing(e -> {
                    List<AgenticFlowExecution> execs = e.getValue();
                    double confidence = calculateAverageConfidence(execs);
                    double success = calculateSuccessRate(execs);
                    return confidence * 0.5 + success * 0.5;
                }))
                .orElse(null);
        
        if (bestOverall != null) {
            return String.format(
                "Based on performance metrics, %s shows the best overall results with %.1f%% average confidence and %.1f%% success rate.",
                bestOverall.getKey(),
                calculateAverageConfidence(bestOverall.getValue()),
                calculateSuccessRate(bestOverall.getValue()) * 100
            );
        }
        
        return "Insufficient data to generate recommendation.";
    }
    
    private Long extractLong(Map<String, Object> metrics, String key) {
        Object value = metrics.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    private Integer extractInt(Map<String, Object> metrics, String key) {
        Object value = metrics.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}