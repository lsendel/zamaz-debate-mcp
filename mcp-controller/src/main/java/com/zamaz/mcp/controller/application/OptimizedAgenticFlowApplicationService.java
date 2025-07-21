package com.zamaz.mcp.controller.application;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.port.out.AgenticFlowRepository;
import com.zamaz.mcp.controller.port.out.LlmServicePort;
import com.zamaz.mcp.controller.domain.analytics.TrendingFlowType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Optimized implementation of AgenticFlowApplicationService with caching and async processing.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OptimizedAgenticFlowApplicationService {
    
    private final AgenticFlowRepository flowRepository;
    private final AgenticFlowExecutor flowExecutor;
    private final AgenticFlowAnalyticsService analyticsService;
    
    @Qualifier("agenticFlowExecutor")
    private final Executor asyncExecutor;
    
    @Qualifier("agenticFlowLLMExecutor")
    private final Executor llmExecutor;
    
    /**
     * Gets a flow configuration with caching.
     */
    @Cacheable(
        value = "agenticFlowConfigurations",
        key = "#flowId",
        condition = "#flowId != null"
    )
    public AgenticFlow getFlow(UUID flowId) {
        log.debug("Loading flow configuration: {}", flowId);
        return flowRepository.findById(new AgenticFlowId(flowId.toString()))
            .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
    }
    
    /**
     * Creates or updates a flow configuration and evicts cache.
     */
    @CacheEvict(
        value = "agenticFlowConfigurations",
        key = "#result.id.value"
    )
    public AgenticFlow saveFlow(AgenticFlow flow) {
        log.debug("Saving flow configuration: {}", flow.getId());
        return flowRepository.save(flow);
    }
    
    /**
     * Executes an agentic flow asynchronously with optimizations.
     */
    @Async("agenticFlowLLMExecutor")
    public CompletableFuture<AgenticFlowResult> executeFlowAsync(
            UUID flowId,
            String prompt,
            ExecutionContext context) {
        
        log.debug("Executing flow {} asynchronously", flowId);
        
        return CompletableFuture
            .supplyAsync(() -> getFlow(flowId), asyncExecutor)
            .thenCompose(flow -> {
                // Check cache for recent similar executions
                String cacheKey = generateExecutionCacheKey(flow, prompt);
                AgenticFlowResult cachedResult = getCachedExecution(cacheKey);
                
                if (cachedResult != null && !context.isForceRefresh()) {
                    log.debug("Returning cached result for flow {}", flowId);
                    return CompletableFuture.completedFuture(cachedResult);
                }
                
                // Execute flow with proper executor based on type
                return executeFlowWithOptimizations(flow, prompt, context);
            })
            .thenApply(result -> {
                // Async analytics recording (fire and forget)
                recordAnalyticsAsync(flowId, result, context);
                return result;
            })
            .exceptionally(throwable -> {
                log.error("Error executing flow {}: {}", flowId, throwable.getMessage());
                return createErrorResult(flowId, throwable);
            });
    }
    
    /**
     * Batch executes multiple flows for better performance.
     */
    @Async("agenticFlowExecutor")
    public CompletableFuture<List<AgenticFlowResult>> executeFlowsBatch(
            List<FlowExecutionRequest> requests) {
        
        log.debug("Batch executing {} flows", requests.size());
        
        // Process in parallel with controlled concurrency
        List<CompletableFuture<AgenticFlowResult>> futures = requests.stream()
            .map(request -> executeFlowAsync(
                request.getFlowId(),
                request.getPrompt(),
                request.getContext()
            ))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        ).thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList())
        );
    }
    
    /**
     * Gets flow statistics with caching.
     */
    @Cacheable(
        value = "flowTypeStatistics",
        key = "#flowType + '_' + #organizationId",
        condition = "#organizationId != null"
    )
    public FlowTypeStatistics getFlowTypeStatistics(
            AgenticFlowType flowType,
            UUID organizationId) {
        
        log.debug("Loading statistics for flow type {} in org {}", flowType, organizationId);
        
        return analyticsService.getFlowTypeAnalytics(
            organizationId,
            flowType,
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now()
        ).join();
    }
    
    /**
     * Warms up caches for frequently used flows.
     */
    @Async("agenticFlowExecutor")
    public void warmUpCaches(UUID organizationId) {
        log.info("Warming up caches for organization {}", organizationId);
        
        CompletableFuture.runAsync(() -> {
            // Load trending flows
            List<TrendingFlowType> trendingFlows = analyticsService
                .getTrendingFlowTypes(organizationId, 5)
                .join();
            
            // Pre-load configurations for trending flows
            trendingFlows.forEach(trending -> {
                try {
                    List<AgenticFlow> flows = flowRepository
                        .findByOrganizationIdAndFlowType(
                            new OrganizationId(organizationId.toString()),
                            trending.getFlowType()
                        );
                    
                    flows.forEach(flow -> {
                        log.debug("Pre-loading flow {}", flow.getId());
                        // This will cache the flow
                        getFlow(UUID.fromString(flow.getId().getValue()));
                    });
                } catch (Exception e) {
                    log.warn("Failed to pre-load flows for type {}", trending.getFlowType(), e);
                }
            });
        }, asyncExecutor);
    }
    
    // Private helper methods
    
    private CompletableFuture<AgenticFlowResult> executeFlowWithOptimizations(
            AgenticFlow flow,
            String prompt,
            ExecutionContext context) {
        
        // Choose appropriate executor based on flow type
        Executor executor = isResourceIntensiveFlow(flow.getFlowType()) 
            ? llmExecutor 
            : asyncExecutor;
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Apply flow-specific optimizations
                if (flow.getFlowType() == AgenticFlowType.RAG_WITH_RERANKING) {
                    // Use parallel retrieval for RAG
                    return executeRagWithParallelRetrieval(flow, prompt, context);
                } else if (flow.getFlowType() == AgenticFlowType.ENSEMBLE_VOTING) {
                    // Use parallel LLM calls for ensemble
                    return executeEnsembleWithParallelCalls(flow, prompt, context);
                } else {
                    // Standard execution
                    return flowExecutor.execute(flow, prompt, context);
                }
            } finally {
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Flow {} executed in {}ms", flow.getId(), executionTime);
            }
        }, executor);
    }
    
    private boolean isResourceIntensiveFlow(AgenticFlowType flowType) {
        return flowType == AgenticFlowType.MULTI_AGENT_RED_TEAM ||
               flowType == AgenticFlowType.TREE_OF_THOUGHTS ||
               flowType == AgenticFlowType.ENSEMBLE_VOTING;
    }
    
    private String generateExecutionCacheKey(AgenticFlow flow, String prompt) {
        // Create a cache key based on flow config and prompt hash
        int promptHash = prompt.hashCode();
        int configHash = flow.getConfiguration().hashCode();
        return String.format("%s_%d_%d", flow.getId().getValue(), promptHash, configHash);
    }
    
    @Cacheable(
        value = "agenticFlowExecutions",
        key = "#cacheKey",
        unless = "#result == null"
    )
    private AgenticFlowResult getCachedExecution(String cacheKey) {
        // This method will be intercepted by Spring cache
        return null;
    }
    
    @Async("agenticFlowAnalyticsExecutor")
    private void recordAnalyticsAsync(
            UUID flowId,
            AgenticFlowResult result,
            ExecutionContext context) {
        
        try {
            analyticsService.recordExecution(
                flowId,
                context.getDebateId(),
                context.getOrganizationId(),
                result,
                Duration.ofMillis(context.getExecutionTime())
            );
        } catch (Exception e) {
            log.error("Failed to record analytics for flow {}", flowId, e);
        }
    }
    
    private AgenticFlowResult createErrorResult(UUID flowId, Throwable error) {
        return AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(flowId.toString()))
            .flowType(AgenticFlowType.UNKNOWN)
            .status(AgenticFlowStatus.FAILED)
            .error(error.getMessage())
            .timestamp(Instant.now())
            .build();
    }
    
    private AgenticFlowResult executeRagWithParallelRetrieval(
            AgenticFlow flow,
            String prompt,
            ExecutionContext context) {
        
        // Implement parallel document retrieval optimization
        // This is a placeholder - actual implementation would use parallel streams
        return flowExecutor.execute(flow, prompt, context);
    }
    
    private AgenticFlowResult executeEnsembleWithParallelCalls(
            AgenticFlow flow,
            String prompt,
            ExecutionContext context) {
        
        // Implement parallel LLM calls for ensemble voting
        // This is a placeholder - actual implementation would use CompletableFuture.allOf
        return flowExecutor.execute(flow, prompt, context);
    }
}