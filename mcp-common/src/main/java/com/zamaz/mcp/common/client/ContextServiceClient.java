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
 * High-level client for Context service MCP tools.
 * Provides type-safe methods for context management operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextServiceClient {

    private final McpServiceClient mcpServiceClient;
    private final McpServiceRegistry serviceRegistry;

    /**
     * Create a new context.
     *
     * @param name Context name
     * @param description Context description (optional)
     * @param authentication Authentication context
     * @return Context creation response
     */
    public JsonNode createContext(String name, String description, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        if (description != null) {
            params.put("description", description);
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.callTool(serviceUrl, "create_context", params, authentication);
    }

    /**
     * Append a message to a context.
     *
     * @param contextId Context ID
     * @param role Message role (user, assistant, system)
     * @param content Message content
     * @param authentication Authentication context
     * @return Message append response
     */
    public JsonNode appendMessage(UUID contextId, String role, String content, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("contextId", contextId.toString());
        params.put("role", role);
        params.put("content", content);

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.callTool(serviceUrl, "append_message", params, authentication);
    }

    /**
     * Get a context window optimized for token limits.
     *
     * @param contextId Context ID
     * @param maxTokens Maximum tokens (default: 4096)
     * @param messageLimit Maximum number of messages (optional)
     * @param includeSystemMessages Include system messages (default: true)
     * @param preserveMessageBoundaries Preserve message boundaries (default: true)
     * @param authentication Authentication context
     * @return Context window response
     */
    public JsonNode getContextWindow(UUID contextId, Integer maxTokens, Integer messageLimit, 
                                   Boolean includeSystemMessages, Boolean preserveMessageBoundaries,
                                   Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("contextId", contextId.toString());
        
        if (maxTokens != null) {
            params.put("maxTokens", maxTokens);
        }
        if (messageLimit != null) {
            params.put("messageLimit", messageLimit);
        }
        if (includeSystemMessages != null) {
            params.put("includeSystemMessages", includeSystemMessages);
        }
        if (preserveMessageBoundaries != null) {
            params.put("preserveMessageBoundaries", preserveMessageBoundaries);
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.callTool(serviceUrl, "get_context_window", params, authentication);
    }

    /**
     * Get a context window with default settings.
     *
     * @param contextId Context ID
     * @param authentication Authentication context
     * @return Context window response
     */
    public JsonNode getContextWindow(UUID contextId, Authentication authentication) {
        return getContextWindow(contextId, null, null, null, null, authentication);
    }

    /**
     * Search contexts by query.
     *
     * @param query Search query
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication context
     * @return Search results
     */
    public JsonNode searchContexts(String query, Integer page, Integer size, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        
        if (page != null) {
            params.put("page", page);
        }
        if (size != null) {
            params.put("size", size);
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.callTool(serviceUrl, "search_contexts", params, authentication);
    }

    /**
     * Search contexts with default pagination.
     *
     * @param query Search query
     * @param authentication Authentication context
     * @return Search results
     */
    public JsonNode searchContexts(String query, Authentication authentication) {
        return searchContexts(query, null, null, authentication);
    }

    /**
     * Share a context with another organization or user.
     *
     * @param contextId Context ID to share
     * @param targetOrganizationId Target organization ID (optional)
     * @param targetUserId Target user ID (optional)
     * @param permission Permission level (read, write, admin)
     * @param authentication Authentication context
     * @return Context sharing response
     */
    public JsonNode shareContext(UUID contextId, UUID targetOrganizationId, UUID targetUserId, 
                               String permission, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("contextId", contextId.toString());
        params.put("permission", permission);
        
        if (targetOrganizationId != null) {
            params.put("targetOrganizationId", targetOrganizationId.toString());
        }
        if (targetUserId != null) {
            params.put("targetUserId", targetUserId.toString());
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.callTool(serviceUrl, "share_context", params, authentication);
    }

    /**
     * Share context with an organization.
     *
     * @param contextId Context ID to share
     * @param targetOrganizationId Target organization ID
     * @param permission Permission level
     * @param authentication Authentication context
     * @return Context sharing response
     */
    public JsonNode shareContextWithOrganization(UUID contextId, UUID targetOrganizationId, 
                                                String permission, Authentication authentication) {
        return shareContext(contextId, targetOrganizationId, null, permission, authentication);
    }

    /**
     * Share context with a user.
     *
     * @param contextId Context ID to share
     * @param targetUserId Target user ID
     * @param permission Permission level
     * @param authentication Authentication context
     * @return Context sharing response
     */
    public JsonNode shareContextWithUser(UUID contextId, UUID targetUserId, 
                                       String permission, Authentication authentication) {
        return shareContext(contextId, null, targetUserId, permission, authentication);
    }

    /**
     * Check if context service is available.
     *
     * @return true if service is available
     */
    public boolean isContextServiceAvailable() {
        return serviceRegistry.isServiceAvailable(McpServiceRegistry.McpService.CONTEXT);
    }

    /**
     * Get available context tools.
     *
     * @return List of available tools
     */
    public JsonNode getAvailableTools() {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.listTools(serviceUrl);
    }

    /**
     * Get context service information.
     *
     * @return Service information
     */
    public JsonNode getServiceInfo() {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT);
        return mcpServiceClient.getServerInfo(serviceUrl);
    }
}