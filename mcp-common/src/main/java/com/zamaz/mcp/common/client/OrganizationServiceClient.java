package com.zamaz.mcp.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * High-level client for Organization service MCP tools.
 * Provides type-safe methods for common organization operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceClient {

    private final McpServiceClient mcpServiceClient;
    private final McpServiceRegistry serviceRegistry;

    /**
     * Create a new organization.
     *
     * @param name Organization name
     * @param description Organization description (optional)
     * @param authentication Authentication context
     * @return Organization creation response
     */
    public JsonNode createOrganization(String name, String description, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        if (description != null) {
            params.put("description", description);
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.callTool(serviceUrl, "create_organization", params, authentication);
    }

    /**
     * Get organization details.
     *
     * @param authentication Authentication context (organization ID extracted from context)
     * @return Organization details
     */
    public JsonNode getOrganization(Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        // Organization ID will be extracted from authentication context by the service

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.callTool(serviceUrl, "get_organization", params, authentication);
    }

    /**
     * Update organization details.
     *
     * @param name New organization name (optional)
     * @param description New organization description (optional)
     * @param isActive Organization active status (optional)
     * @param authentication Authentication context
     * @return Updated organization details
     */
    public JsonNode updateOrganization(String name, String description, Boolean isActive, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        if (name != null) {
            params.put("name", name);
        }
        if (description != null) {
            params.put("description", description);
        }
        if (isActive != null) {
            params.put("isActive", isActive);
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.callTool(serviceUrl, "update_organization", params, authentication);
    }

    /**
     * Delete organization.
     *
     * @param authentication Authentication context (must have ADMIN role)
     * @return Deletion confirmation
     */
    public JsonNode deleteOrganization(Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        // Organization ID will be extracted from authentication context

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.callTool(serviceUrl, "delete_organization", params, authentication);
    }

    /**
     * Add a user to the organization.
     *
     * @param userId User ID to add
     * @param role User role (default: "member")
     * @param authentication Authentication context (must have ADMIN role)
     * @return Addition confirmation
     */
    public JsonNode addUserToOrganization(UUID userId, String role, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId.toString());
        params.put("role", role != null ? role : "member");

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.callTool(serviceUrl, "add_user_to_organization", params, authentication);
    }

    /**
     * Remove a user from the organization.
     *
     * @param userId User ID to remove
     * @param authentication Authentication context (must have ADMIN role)
     * @return Removal confirmation
     */
    public JsonNode removeUserFromOrganization(UUID userId, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId.toString());

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.callTool(serviceUrl, "remove_user_from_organization", params, authentication);
    }

    /**
     * List organizations (returns user's organization for security).
     *
     * @param authentication Authentication context
     * @return Organization list
     */
    public JsonNode listOrganizations(Authentication authentication) {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        
        // This is a GET endpoint for MCP resources
        try {
            return mcpServiceClient.callTool(serviceUrl, "list_organizations", new HashMap<>(), authentication);
        } catch (Exception e) {
            log.warn("Failed to call list_organizations as tool, trying resource endpoint");
            // Fallback or alternative approach could be implemented here
            throw e;
        }
    }

    /**
     * Check if organization service is available.
     *
     * @return true if service is available
     */
    public boolean isOrganizationServiceAvailable() {
        return serviceRegistry.isServiceAvailable(McpServiceRegistry.McpService.ORGANIZATION);
    }

    /**
     * Get available organization tools.
     *
     * @return List of available tools
     */
    public JsonNode getAvailableTools() {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.listTools(serviceUrl);
    }

    /**
     * Get organization service information.
     *
     * @return Service information
     */
    public JsonNode getServiceInfo() {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION);
        return mcpServiceClient.getServerInfo(serviceUrl);
    }
}