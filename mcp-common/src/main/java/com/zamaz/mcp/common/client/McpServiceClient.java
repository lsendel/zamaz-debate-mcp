package com.zamaz.mcp.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.common.error.McpErrorCode;
import com.zamaz.mcp.common.error.McpErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic MCP client for inter-service communication.
 * Provides a standardized way for services to call MCP tools on other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpServiceClient {

    private final RestTemplate resilientRestTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Call an MCP tool on another service.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @param toolName Name of the MCP tool to call
     * @param params Parameters for the tool
     * @param authentication Authentication context
     * @return Tool response as JsonNode
     * @throws McpClientException if the call fails
     */
    public JsonNode callTool(String serviceBaseUrl, String toolName, Map<String, Object> params, Authentication authentication) {
        return callTool(serviceBaseUrl, toolName, params, authentication, null);
    }

    /**
     * Call an MCP tool on another service with request ID.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @param toolName Name of the MCP tool to call
     * @param params Parameters for the tool
     * @param authentication Authentication context
     * @param requestId Optional request ID for tracking
     * @return Tool response as JsonNode
     * @throws McpClientException if the call fails
     */
    public JsonNode callTool(String serviceBaseUrl, String toolName, Map<String, Object> params, 
                            Authentication authentication, String requestId) {
        
        String actualRequestId = requestId != null ? requestId : UUID.randomUUID().toString();
        
        log.debug("Calling MCP tool '{}' on service '{}' with request ID: {}", toolName, serviceBaseUrl, actualRequestId);

        try {
            // Create MCP tool call request
            ObjectNode mcpRequest = objectMapper.createObjectNode();
            mcpRequest.put("name", toolName);
            mcpRequest.set("arguments", objectMapper.valueToTree(params));
            mcpRequest.put("requestId", actualRequestId);

            // Build HTTP headers with authentication
            HttpHeaders headers = createAuthenticatedHeaders(authentication);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Request-ID", actualRequestId);

            HttpEntity<JsonNode> requestEntity = new HttpEntity<>(mcpRequest, headers);

            // Determine the endpoint URL
            String callToolUrl = normalizeServiceUrl(serviceBaseUrl) + "/mcp/call-tool";

            // Make the HTTP call
            ResponseEntity<JsonNode> response = resilientRestTemplate.exchange(
                callToolUrl,
                HttpMethod.POST,
                requestEntity,
                JsonNode.class
            );

            // Validate response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseBody = response.getBody();
                
                // Check for error in response body
                if (responseBody.has("error")) {
                    String errorMessage = responseBody.get("error").asText();
                    log.warn("MCP tool '{}' returned error: {}", toolName, errorMessage);
                    throw new McpClientException(
                        McpErrorCode.TOOL_EXECUTION_FAILED,
                        "Tool execution failed: " + errorMessage,
                        actualRequestId
                    );
                }
                
                log.debug("Successfully called MCP tool '{}' on service '{}'", toolName, serviceBaseUrl);
                return responseBody;
            } else {
                log.error("MCP tool call failed with status: {}", response.getStatusCode());
                throw new McpClientException(
                    McpErrorCode.SERVICE_UNAVAILABLE,
                    "Service returned non-success status: " + response.getStatusCode(),
                    actualRequestId
                );
            }

        } catch (RestClientException e) {
            log.error("HTTP error calling MCP tool '{}' on service '{}': {}", toolName, serviceBaseUrl, e.getMessage());
            throw new McpClientException(
                McpErrorCode.SERVICE_UNAVAILABLE,
                "Service communication failed: " + e.getMessage(),
                actualRequestId,
                e
            );
        } catch (Exception e) {
            log.error("Unexpected error calling MCP tool '{}' on service '{}': {}", toolName, serviceBaseUrl, e.getMessage(), e);
            throw new McpClientException(
                McpErrorCode.INTERNAL_ERROR,
                "Unexpected error during tool call: " + e.getMessage(),
                actualRequestId,
                e
            );
        }
    }

    /**
     * Call an MCP tool synchronously using current authentication context.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @param toolName Name of the MCP tool to call
     * @param params Parameters for the tool
     * @return Tool response as JsonNode
     * @throws McpClientException if the call fails
     */
    public JsonNode callTool(String serviceBaseUrl, String toolName, Map<String, Object> params) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new McpClientException(
                McpErrorCode.AUTHENTICATION_REQUIRED,
                "No authentication context available for MCP call",
                UUID.randomUUID().toString()
            );
        }
        return callTool(serviceBaseUrl, toolName, params, authentication);
    }

    /**
     * List available tools on a service.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @return List of available tools
     */
    public JsonNode listTools(String serviceBaseUrl) {
        return listTools(serviceBaseUrl, SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * List available tools on a service with authentication.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @param authentication Authentication context
     * @return List of available tools
     */
    public JsonNode listTools(String serviceBaseUrl, Authentication authentication) {
        log.debug("Listing MCP tools on service: {}", serviceBaseUrl);

        try {
            // Build HTTP headers with authentication
            HttpHeaders headers = createAuthenticatedHeaders(authentication);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // Call list-tools endpoint
            String listToolsUrl = normalizeServiceUrl(serviceBaseUrl) + "/mcp/list-tools";

            ResponseEntity<JsonNode> response = resilientRestTemplate.exchange(
                listToolsUrl,
                HttpMethod.POST,
                requestEntity,
                JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully listed tools from service: {}", serviceBaseUrl);
                return response.getBody();
            } else {
                log.error("Failed to list tools from service: {} - Status: {}", serviceBaseUrl, response.getStatusCode());
                throw new McpClientException(
                    McpErrorCode.SERVICE_UNAVAILABLE,
                    "Failed to list tools: " + response.getStatusCode(),
                    UUID.randomUUID().toString()
                );
            }

        } catch (RestClientException e) {
            log.error("HTTP error listing tools from service '{}': {}", serviceBaseUrl, e.getMessage());
            throw new McpClientException(
                McpErrorCode.SERVICE_UNAVAILABLE,
                "Service communication failed: " + e.getMessage(),
                UUID.randomUUID().toString(),
                e
            );
        }
    }

    /**
     * Check if a service is available and responding.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @return true if service is available
     */
    public boolean isServiceAvailable(String serviceBaseUrl) {
        try {
            String healthUrl = normalizeServiceUrl(serviceBaseUrl) + "/health";
            ResponseEntity<String> response = resilientRestTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Service not available: {} - {}", serviceBaseUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Get server information from a service.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @return Server information
     */
    public JsonNode getServerInfo(String serviceBaseUrl) {
        return getServerInfo(serviceBaseUrl, SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Get server information from a service with authentication.
     *
     * @param serviceBaseUrl Base URL of the target service
     * @param authentication Authentication context
     * @return Server information
     */
    public JsonNode getServerInfo(String serviceBaseUrl, Authentication authentication) {
        log.debug("Getting server info from service: {}", serviceBaseUrl);

        try {
            HttpHeaders headers = createAuthenticatedHeaders(authentication);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            String infoUrl = normalizeServiceUrl(serviceBaseUrl) + "/mcp";

            ResponseEntity<JsonNode> response = resilientRestTemplate.exchange(
                infoUrl,
                HttpMethod.GET,
                requestEntity,
                JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new McpClientException(
                    McpErrorCode.SERVICE_UNAVAILABLE,
                    "Failed to get server info: " + response.getStatusCode(),
                    UUID.randomUUID().toString()
                );
            }

        } catch (RestClientException e) {
            throw new McpClientException(
                McpErrorCode.SERVICE_UNAVAILABLE,
                "Service communication failed: " + e.getMessage(),
                UUID.randomUUID().toString(),
                e
            );
        }
    }

    /**
     * Create HTTP headers with authentication from the security context.
     */
    private HttpHeaders createAuthenticatedHeaders(Authentication authentication) {
        HttpHeaders headers = new HttpHeaders();
        
        if (authentication != null) {
            // Handle JWT token
            if (authentication.getCredentials() instanceof String) {
                String token = (String) authentication.getCredentials();
                headers.setBearerAuth(token);
            } else if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            
            // Add user context headers
            headers.set("X-User-ID", authentication.getName());
            
            // Extract organization ID if available
            if (authentication.getAuthorities() != null) {
                authentication.getAuthorities().stream()
                    .filter(auth -> auth.getAuthority().startsWith("ORG_"))
                    .findFirst()
                    .ifPresent(orgAuth -> {
                        String orgId = orgAuth.getAuthority().substring(4); // Remove "ORG_" prefix
                        headers.set("X-Organization-ID", orgId);
                    });
            }
        }

        return headers;
    }

    /**
     * Normalize service URL to ensure proper formatting.
     */
    private String normalizeServiceUrl(String serviceBaseUrl) {
        if (serviceBaseUrl == null || serviceBaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Service base URL cannot be null or empty");
        }
        
        String normalized = serviceBaseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        return normalized;
    }
}