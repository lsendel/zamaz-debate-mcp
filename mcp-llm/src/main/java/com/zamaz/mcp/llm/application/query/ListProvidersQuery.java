package com.zamaz.mcp.llm.application.query;

import com.zamaz.mcp.common.application.query.Query;
import com.zamaz.mcp.llm.domain.model.LlmModel.ModelCapability;
import com.zamaz.mcp.llm.domain.model.ProviderStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Query to list LLM providers with optional filtering.
 * Supports filtering by status, capabilities, and organization.
 */
public record ListProvidersQuery(
    String organizationId,
    Optional<ProviderStatus> statusFilter,
    Optional<Set<ModelCapability>> requiredCapabilities,
    Optional<String> nameFilter,
    boolean includeModels,
    boolean includeMetrics,
    int limit,
    int offset
) implements Query {
    
    public ListProvidersQuery {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(statusFilter, "Status filter cannot be null");
        Objects.requireNonNull(requiredCapabilities, "Required capabilities cannot be null");
        Objects.requireNonNull(nameFilter, "Name filter cannot be null");
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        if (limit > 100) {
            throw new IllegalArgumentException("Limit cannot exceed 100");
        }
    }
    
    /**
     * Creates a basic query to list all providers for an organization.
     * 
     * @param organizationId the organization ID
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery forOrganization(String organizationId) {
        return new ListProvidersQuery(
            organizationId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false, // Don't include models by default
            false, // Don't include metrics by default
            50,    // Default limit
            0      // Default offset
        );
    }
    
    /**
     * Creates a query filtered by provider status.
     * 
     * @param organizationId the organization ID
     * @param status the required provider status
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery withStatus(String organizationId, ProviderStatus status) {
        return new ListProvidersQuery(
            organizationId,
            Optional.of(status),
            Optional.empty(),
            Optional.empty(),
            false,
            false,
            50,
            0
        );
    }
    
    /**
     * Creates a query filtered by required capabilities.
     * 
     * @param organizationId the organization ID
     * @param capabilities the required capabilities
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery withCapabilities(String organizationId, Set<ModelCapability> capabilities) {
        return new ListProvidersQuery(
            organizationId,
            Optional.empty(),
            Optional.of(capabilities),
            Optional.empty(),
            false,
            false,
            50,
            0
        );
    }
    
    /**
     * Creates a query that includes model information.
     * 
     * @param organizationId the organization ID
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery withModels(String organizationId) {
        return new ListProvidersQuery(
            organizationId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true, // Include models
            false,
            50,
            0
        );
    }
    
    /**
     * Creates a comprehensive query with all information.
     * 
     * @param organizationId the organization ID
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery comprehensive(String organizationId) {
        return new ListProvidersQuery(
            organizationId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true, // Include models
            true, // Include metrics
            50,
            0
        );
    }
    
    /**
     * Creates a paginated query.
     * 
     * @param organizationId the organization ID
     * @param limit the maximum number of results
     * @param offset the offset for pagination
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery paginated(String organizationId, int limit, int offset) {
        return new ListProvidersQuery(
            organizationId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false,
            false,
            limit,
            offset
        );
    }
    
    /**
     * Creates a query filtered by provider name.
     * 
     * @param organizationId the organization ID
     * @param nameFilter the name filter (supports partial matching)
     * @return a new ListProvidersQuery
     */
    public static ListProvidersQuery withNameFilter(String organizationId, String nameFilter) {
        return new ListProvidersQuery(
            organizationId,
            Optional.empty(),
            Optional.empty(),
            Optional.of(nameFilter),
            false,
            false,
            50,
            0
        );
    }
    
    /**
     * Creates a query for healthy providers only.
     * 
     * @param organizationId the organization ID
     * @return a new ListProvidersQuery for available providers
     */
    public static ListProvidersQuery healthyOnly(String organizationId) {
        return withStatus(organizationId, ProviderStatus.AVAILABLE);
    }
}