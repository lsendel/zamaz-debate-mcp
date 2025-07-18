package com.zamaz.mcp.llm.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Objects;

/**
 * Command to check the health status of a specific LLM provider.
 * This command initiates a health check operation for provider monitoring.
 */
public record CheckProviderHealthCommand(
    String providerId,
    String organizationId,
    boolean includeModelStatus,
    boolean forceRefresh
) implements Command {
    
    public CheckProviderHealthCommand {
        Objects.requireNonNull(providerId, "Provider ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        if (providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be empty");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
    }
    
    /**
     * Creates a basic health check command.
     * 
     * @param providerId the ID of the provider to check
     * @param organizationId the organization requesting the check
     * @return a new CheckProviderHealthCommand
     */
    public static CheckProviderHealthCommand of(String providerId, String organizationId) {
        return new CheckProviderHealthCommand(
            providerId,
            organizationId,
            false, // Default: don't include model status
            false  // Default: use cached results if available
        );
    }
    
    /**
     * Creates a comprehensive health check command that includes model status.
     * 
     * @param providerId the ID of the provider to check
     * @param organizationId the organization requesting the check
     * @return a new CheckProviderHealthCommand with model status included
     */
    public static CheckProviderHealthCommand withModelStatus(String providerId, String organizationId) {
        return new CheckProviderHealthCommand(
            providerId,
            organizationId,
            true, // Include model status
            false
        );
    }
    
    /**
     * Creates a forced health check command that bypasses cache.
     * 
     * @param providerId the ID of the provider to check
     * @param organizationId the organization requesting the check
     * @return a new CheckProviderHealthCommand that forces a fresh check
     */
    public static CheckProviderHealthCommand forceRefresh(String providerId, String organizationId) {
        return new CheckProviderHealthCommand(
            providerId,
            organizationId,
            false,
            true // Force refresh
        );
    }
    
    /**
     * Creates a comprehensive forced health check command.
     * 
     * @param providerId the ID of the provider to check
     * @param organizationId the organization requesting the check
     * @return a new CheckProviderHealthCommand with all options enabled
     */
    public static CheckProviderHealthCommand comprehensive(String providerId, String organizationId) {
        return new CheckProviderHealthCommand(
            providerId,
            organizationId,
            true, // Include model status
            true  // Force refresh
        );
    }
}