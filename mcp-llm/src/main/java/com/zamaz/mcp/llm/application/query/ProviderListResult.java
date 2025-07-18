package com.zamaz.mcp.llm.application.query;

import com.zamaz.mcp.llm.domain.model.LlmModel;
import com.zamaz.mcp.llm.domain.model.LlmModel.ModelCapability;
import com.zamaz.mcp.llm.domain.model.ProviderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result DTO for provider listing.
 * Contains provider information, available models, and operational metrics.
 */
public record ProviderListResult(
    List<ProviderInfo> providers,
    int totalCount,
    boolean hasMore,
    String nextCursor,
    Instant generatedAt,
    Map<String, Object> aggregatedMetrics
) {
    
    public ProviderListResult {
        Objects.requireNonNull(providers, "Providers list cannot be null");
        Objects.requireNonNull(nextCursor, "Next cursor cannot be null");
        Objects.requireNonNull(generatedAt, "Generated at cannot be null");
        Objects.requireNonNull(aggregatedMetrics, "Aggregated metrics cannot be null");
        
        if (totalCount < 0) {
            throw new IllegalArgumentException("Total count cannot be negative");
        }
        
        if (providers.size() > totalCount) {
            throw new IllegalArgumentException("Providers list size cannot exceed total count");
        }
    }
    
    /**
     * Creates a basic provider list result.
     * 
     * @param providers the list of providers
     * @param totalCount the total number of providers available
     * @return a new ProviderListResult
     */
    public static ProviderListResult of(List<ProviderInfo> providers, int totalCount) {
        return new ProviderListResult(
            List.copyOf(providers),
            totalCount,
            false, // No pagination by default
            "",    // No cursor
            Instant.now(),
            Map.of()
        );
    }
    
    /**
     * Creates a paginated provider list result.
     * 
     * @param providers the list of providers for this page
     * @param totalCount the total number of providers available
     * @param hasMore whether there are more results available
     * @param nextCursor the cursor for the next page
     * @return a new ProviderListResult with pagination info
     */
    public static ProviderListResult paginated(
            List<ProviderInfo> providers,
            int totalCount,
            boolean hasMore,
            String nextCursor
    ) {
        return new ProviderListResult(
            List.copyOf(providers),
            totalCount,
            hasMore,
            nextCursor,
            Instant.now(),
            Map.of()
        );
    }
    
    /**
     * Creates a provider list result with aggregated metrics.
     * 
     * @param providers the list of providers
     * @param totalCount the total number of providers available
     * @param aggregatedMetrics system-wide metrics
     * @return a new ProviderListResult with metrics
     */
    public static ProviderListResult withMetrics(
            List<ProviderInfo> providers,
            int totalCount,
            Map<String, Object> aggregatedMetrics
    ) {
        return new ProviderListResult(
            List.copyOf(providers),
            totalCount,
            false,
            "",
            Instant.now(),
            Map.copyOf(aggregatedMetrics)
        );
    }
    
    /**
     * Gets the number of providers in this result.
     * 
     * @return the number of providers
     */
    public int getProviderCount() {
        return providers.size();
    }
    
    /**
     * Checks if this result is empty.
     * 
     * @return true if no providers are in this result
     */
    public boolean isEmpty() {
        return providers.isEmpty();
    }
    
    /**
     * Gets providers with a specific status.
     * 
     * @param status the desired provider status
     * @return list of providers with the specified status
     */
    public List<ProviderInfo> getProvidersByStatus(ProviderStatus status) {
        return providers.stream()
            .filter(p -> p.status() == status)
            .toList();
    }
    
    /**
     * Gets the count of healthy providers.
     * 
     * @return number of healthy providers
     */
    public long getHealthyProviderCount() {
        return providers.stream()
            .filter(p -> p.status().isHealthy())
            .count();
    }
    
    /**
     * Nested record representing individual provider information.
     */
    public record ProviderInfo(
        String providerId,
        String name,
        String description,
        ProviderStatus status,
        Set<ModelCapability> capabilities,
        List<ModelInfo> models,
        ProviderMetrics metrics,
        Instant lastHealthCheck,
        Map<String, Object> configuration
    ) {
        
        public ProviderInfo {
            Objects.requireNonNull(providerId, "Provider ID cannot be null");
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(description, "Description cannot be null");
            Objects.requireNonNull(status, "Status cannot be null");
            Objects.requireNonNull(capabilities, "Capabilities cannot be null");
            Objects.requireNonNull(models, "Models cannot be null");
            Objects.requireNonNull(metrics, "Metrics cannot be null");
            Objects.requireNonNull(lastHealthCheck, "Last health check cannot be null");
            Objects.requireNonNull(configuration, "Configuration cannot be null");
        }
        
        /**
         * Creates basic provider info.
         */
        public static ProviderInfo of(
                String providerId,
                String name,
                String description,
                ProviderStatus status
        ) {
            return new ProviderInfo(
                providerId,
                name,
                description,
                status,
                Set.of(),
                List.of(),
                ProviderMetrics.empty(),
                Instant.now(),
                Map.of()
            );
        }
        
        /**
         * Checks if this provider supports a specific capability.
         */
        public boolean hasCapability(ModelCapability capability) {
            return capabilities.contains(capability) || 
                   models.stream().anyMatch(m -> m.capabilities().contains(capability));
        }
        
        /**
         * Gets models that support a specific capability.
         */
        public List<ModelInfo> getModelsWithCapability(ModelCapability capability) {
            return models.stream()
                .filter(m -> m.capabilities().contains(capability))
                .toList();
        }
    }
    
    /**
     * Nested record representing model information.
     */
    public record ModelInfo(
        String modelName,
        String displayName,
        int maxTokens,
        Set<ModelCapability> capabilities,
        BigDecimal inputTokenCost,
        BigDecimal outputTokenCost,
        LlmModel.ModelStatus status
    ) {
        
        public ModelInfo {
            Objects.requireNonNull(modelName, "Model name cannot be null");
            Objects.requireNonNull(displayName, "Display name cannot be null");
            Objects.requireNonNull(capabilities, "Capabilities cannot be null");
            Objects.requireNonNull(inputTokenCost, "Input token cost cannot be null");
            Objects.requireNonNull(outputTokenCost, "Output token cost cannot be null");
            Objects.requireNonNull(status, "Status cannot be null");
        }
        
        /**
         * Creates basic model info.
         */
        public static ModelInfo of(String modelName, String displayName, int maxTokens) {
            return new ModelInfo(
                modelName,
                displayName,
                maxTokens,
                Set.of(ModelCapability.TEXT_COMPLETION),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LlmModel.ModelStatus.AVAILABLE
            );
        }
    }
    
    /**
     * Nested record representing provider metrics.
     */
    public record ProviderMetrics(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double averageResponseTimeMs,
        double successRate,
        Instant lastRequestAt
    ) {
        
        public ProviderMetrics {
            if (totalRequests < 0) {
                throw new IllegalArgumentException("Total requests cannot be negative");
            }
            if (successfulRequests < 0) {
                throw new IllegalArgumentException("Successful requests cannot be negative");
            }
            if (failedRequests < 0) {
                throw new IllegalArgumentException("Failed requests cannot be negative");
            }
            if (averageResponseTimeMs < 0) {
                throw new IllegalArgumentException("Average response time cannot be negative");
            }
            if (successRate < 0.0 || successRate > 1.0) {
                throw new IllegalArgumentException("Success rate must be between 0.0 and 1.0");
            }
        }
        
        /**
         * Creates empty metrics.
         */
        public static ProviderMetrics empty() {
            return new ProviderMetrics(0, 0, 0, 0.0, 0.0, null);
        }
        
        /**
         * Creates basic metrics.
         */
        public static ProviderMetrics of(long total, long successful, double avgResponseTime) {
            long failed = total - successful;
            double successRate = total > 0 ? (double) successful / total : 0.0;
            return new ProviderMetrics(total, successful, failed, avgResponseTime, successRate, Instant.now());
        }
    }
}