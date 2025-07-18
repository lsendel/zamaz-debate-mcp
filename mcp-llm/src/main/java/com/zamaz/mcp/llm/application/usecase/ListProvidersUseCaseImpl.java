package com.zamaz.mcp.llm.application.usecase;

import com.zamaz.mcp.llm.application.port.inbound.ListProvidersUseCase;
import com.zamaz.mcp.llm.application.port.outbound.ProviderRepository;
import com.zamaz.mcp.llm.application.query.ListProvidersQuery;
import com.zamaz.mcp.llm.application.query.ProviderListResult;
import com.zamaz.mcp.llm.domain.model.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the list providers use case.
 * Orchestrates provider retrieval, filtering, and result mapping.
 */
public class ListProvidersUseCaseImpl implements ListProvidersUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ListProvidersUseCaseImpl.class);
    private static final Duration DEFAULT_HEALTH_CHECK_MAX_AGE = Duration.ofMinutes(5);
    
    private final ProviderRepository providerRepository;
    
    public ListProvidersUseCaseImpl(ProviderRepository providerRepository) {
        this.providerRepository = Objects.requireNonNull(providerRepository, "Provider repository cannot be null");
    }
    
    @Override
    public ProviderListResult execute(ListProvidersQuery query) {
        logger.info("Listing providers for organization: {} with filters: status={}, capabilities={}, name={}", 
            query.organizationId(), 
            query.statusFilter().orElse(null),
            query.requiredCapabilities().map(Set::size).orElse(0),
            query.nameFilter().orElse("none"));
        
        Instant startTime = Instant.now();
        
        try {
            // Get all providers (since no organization filtering is available in repository yet)
            List<Provider> allProviders = providerRepository.findAllOrderedByPriority();
            
            // Apply filtering
            List<Provider> filteredProviders = applyFilters(allProviders, query);
            
            // Apply pagination
            int totalCount = filteredProviders.size();
            List<Provider> paginatedProviders = applyPagination(filteredProviders, query);
            
            // Map to result DTOs
            List<ProviderListResult.ProviderInfo> providerInfos = paginatedProviders.stream()
                .map(provider -> mapToProviderInfo(provider, query))
                .collect(Collectors.toList());
            
            // Calculate pagination info
            boolean hasMore = (query.offset() + query.limit()) < totalCount;
            String nextCursor = hasMore ? generateNextCursor(query) : "";
            
            // Generate aggregated metrics if requested
            Map<String, Object> aggregatedMetrics = query.includeMetrics() 
                ? generateAggregatedMetrics(allProviders)
                : Map.of();
            
            ProviderListResult result = new ProviderListResult(
                providerInfos,
                totalCount,
                hasMore,
                nextCursor,
                startTime,
                aggregatedMetrics
            );
            
            logger.info("Listed {} providers (total: {}) for organization: {} in {}ms", 
                providerInfos.size(), totalCount, query.organizationId(),
                java.time.Duration.between(startTime, Instant.now()).toMillis());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to list providers for organization: {}: {}", 
                query.organizationId(), e.getMessage(), e);
            throw new RuntimeException("Provider listing failed: " + e.getMessage(), e);
        }
    }
    
    private List<Provider> applyFilters(List<Provider> providers, ListProvidersQuery query) {
        return providers.stream()
            .filter(provider -> matchesStatusFilter(provider, query.statusFilter()))
            .filter(provider -> matchesCapabilityFilter(provider, query.requiredCapabilities()))
            .filter(provider -> matchesNameFilter(provider, query.nameFilter()))
            .collect(Collectors.toList());
    }
    
    private boolean matchesStatusFilter(Provider provider, Optional<ProviderStatus> statusFilter) {
        return statusFilter.isEmpty() || provider.getStatus() == statusFilter.get();
    }
    
    private boolean matchesCapabilityFilter(
            Provider provider, 
            Optional<Set<LlmModel.ModelCapability>> requiredCapabilities
    ) {
        if (requiredCapabilities.isEmpty()) {
            return true;
        }
        
        Set<LlmModel.ModelCapability> providerCapabilities = provider.getModels().values().stream()
            .flatMap(model -> getModelCapabilities(model).stream())
            .collect(Collectors.toSet());
        
        return providerCapabilities.containsAll(requiredCapabilities.get());
    }
    
    private boolean matchesNameFilter(Provider provider, Optional<String> nameFilter) {
        return nameFilter.isEmpty() || 
               provider.getName().toLowerCase().contains(nameFilter.get().toLowerCase());
    }
    
    private List<Provider> applyPagination(List<Provider> providers, ListProvidersQuery query) {
        int fromIndex = Math.min(query.offset(), providers.size());
        int toIndex = Math.min(query.offset() + query.limit(), providers.size());
        
        if (fromIndex >= toIndex) {
            return Collections.emptyList();
        }
        
        return providers.subList(fromIndex, toIndex);
    }
    
    private ProviderListResult.ProviderInfo mapToProviderInfo(Provider provider, ListProvidersQuery query) {
        // Map models if requested
        List<ProviderListResult.ModelInfo> modelInfos = query.includeModels()
            ? provider.getModels().values().stream()
                .map(this::mapToModelInfo)
                .collect(Collectors.toList())
            : Collections.emptyList();
        
        // Generate provider metrics if requested
        ProviderListResult.ProviderMetrics metrics = query.includeMetrics()
            ? generateProviderMetrics(provider)
            : ProviderListResult.ProviderMetrics.empty();
        
        // Get provider capabilities
        Set<LlmModel.ModelCapability> capabilities = provider.getModels().values().stream()
            .flatMap(model -> getModelCapabilities(model).stream())
            .collect(Collectors.toSet());
        
        // Get configuration (without sensitive data) - using empty map for now
        Map<String, Object> configuration = Map.of();
        
        return new ProviderListResult.ProviderInfo(
            provider.getProviderId().value(),
            provider.getName(),
            provider.getDescription(),
            provider.getStatus(),
            capabilities,
            modelInfos,
            metrics,
            provider.getLastHealthCheck() != null ? provider.getLastHealthCheck() : Instant.now(),
            configuration
        );
    }
    
    private ProviderListResult.ModelInfo mapToModelInfo(LlmModel model) {
        return new ProviderListResult.ModelInfo(
            model.getModelName().value(),
            model.getDisplayName(),
            model.getMaxTokens(),
            Set.copyOf(getModelCapabilities(model)),
            model.getInputTokenCost(),
            model.getOutputTokenCost(),
            model.getStatus()
        );
    }
    
    private ProviderListResult.ProviderMetrics generateProviderMetrics(Provider provider) {
        // In a real implementation, this would query actual metrics from a metrics store
        // For now, we'll return basic metrics based on provider status and health checks
        
        long totalRequests = 0L; // Would come from metrics store
        long successfulRequests = 0L; // Would come from metrics store
        double avgResponseTime = 0.0; // Would come from metrics store
        
        return ProviderListResult.ProviderMetrics.of(totalRequests, successfulRequests, avgResponseTime);
    }
    
    private Map<String, Object> generateAggregatedMetrics(List<Provider> allProviders) {
        long totalProviders = allProviders.size();
        long healthyProviders = allProviders.stream()
            .filter(p -> p.getStatus().isHealthy())
            .count();
        
        long totalModels = allProviders.stream()
            .mapToLong(p -> p.getModels().size())
            .sum();
        
        Set<LlmModel.ModelCapability> allCapabilities = allProviders.stream()
            .flatMap(p -> p.getModels().values().stream())
            .flatMap(m -> getModelCapabilities(m).stream())
            .collect(Collectors.toSet());
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_providers", totalProviders);
        metrics.put("healthy_providers", healthyProviders);
        metrics.put("health_percentage", totalProviders > 0 ? (double) healthyProviders / totalProviders * 100 : 0.0);
        metrics.put("total_models", totalModels);
        metrics.put("unique_capabilities", allCapabilities.size());
        metrics.put("capabilities", allCapabilities.stream().map(Enum::name).collect(Collectors.toList()));
        metrics.put("generated_at", Instant.now());
        
        return metrics;
    }
    
    private Map<String, Object> sanitizeConfiguration(Map<String, Object> configuration) {
        // Remove sensitive configuration values
        Map<String, Object> sanitized = new HashMap<>(configuration);
        
        // Remove common sensitive keys
        List<String> sensitiveKeys = Arrays.asList(
            "api_key", "apiKey", "secret", "password", "token", "auth", "credential"
        );
        
        sensitiveKeys.forEach(key -> {
            sanitized.remove(key);
            // Also check for keys containing these terms
            sanitized.entrySet().removeIf(entry -> 
                entry.getKey().toLowerCase().contains(key.toLowerCase())
            );
        });
        
        // Replace sensitive values with placeholders
        sanitized.entrySet().forEach(entry -> {
            if (entry.getValue() instanceof String && 
                ((String) entry.getValue()).length() > 20) {
                // Potentially sensitive long strings
                sanitized.put(entry.getKey(), "[REDACTED]");
            }
        });
        
        return sanitized;
    }
    
    private String generateNextCursor(ListProvidersQuery query) {
        // Simple cursor implementation using offset
        int nextOffset = query.offset() + query.limit();
        return Base64.getEncoder().encodeToString(
            String.valueOf(nextOffset).getBytes()
        );
    }
    
    /**
     * Utility method to get all capabilities for a model.
     * Since LlmModel doesn't have a getCapabilities() method,
     * we check each capability individually.
     */
    private Set<LlmModel.ModelCapability> getModelCapabilities(LlmModel model) {
        Set<LlmModel.ModelCapability> capabilities = new HashSet<>();
        
        // Check each capability
        for (LlmModel.ModelCapability capability : LlmModel.ModelCapability.values()) {
            if (model.hasCapability(capability)) {
                capabilities.add(capability);
            }
        }
        
        return capabilities;
    }
}