package com.zamaz.mcp.llm.application.port.outbound;

import com.zamaz.mcp.common.application.port.outbound.Repository;
import com.zamaz.mcp.llm.domain.model.*;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Provider aggregate persistence.
 * This is an outbound port in hexagonal architecture.
 */
public interface ProviderRepository extends Repository<Provider, ProviderId> {
    
    /**
     * Find all providers ordered by priority.
     */
    List<Provider> findAllOrderedByPriority();
    
    /**
     * Find all healthy providers that can accept requests.
     */
    List<Provider> findHealthyProviders();
    
    /**
     * Find providers that support a specific model capability.
     */
    List<Provider> findProvidersWithCapability(LlmModel.ModelCapability capability);
    
    /**
     * Find a provider by name (case-insensitive).
     */
    Optional<Provider> findByName(String name);
    
    /**
     * Find providers that have a specific model available.
     */
    List<Provider> findProvidersWithModel(ModelName modelName);
    
    /**
     * Find the best provider for a request based on token requirements and capabilities.
     */
    Optional<Provider> findBestProviderForRequest(
        int requiredTokens,
        LlmModel.ModelCapability... capabilities
    );
    
    /**
     * Count providers by status.
     */
    long countByStatus(ProviderStatus status);
    
    /**
     * Find providers that haven't been health checked recently.
     */
    List<Provider> findProvidersNeedingHealthCheck(java.time.Instant olderThan);
}