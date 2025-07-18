package com.zamaz.mcp.context.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.common.security.McpSecurityService;
import com.zamaz.mcp.common.security.McpSecurityException;
import com.zamaz.mcp.common.error.McpErrorHandler;
import com.zamaz.mcp.common.resilience.McpRateLimit;
import com.zamaz.mcp.context.dto.AppendMessageRequest;
import com.zamaz.mcp.context.dto.ContextDto;
import com.zamaz.mcp.context.dto.ContextWindowRequest;
import com.zamaz.mcp.context.dto.ContextWindowResponse;
import com.zamaz.mcp.context.dto.CreateContextRequest;
import com.zamaz.mcp.context.dto.MessageDto;
import com.zamaz.mcp.context.dto.ShareContextRequest;
import com.zamaz.mcp.context.entity.SharedContext;
import com.zamaz.mcp.context.service.ContextService;
import com.zamaz.mcp.context.service.ContextSharingService;
import com.zamaz.mcp.context.service.ContextWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCP endpoint controller for the Context service.
 * Provides secure MCP protocol endpoints with proper authentication and organization validation.
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MCP Context Tools", description = "MCP protocol tool endpoints for context management")
public class McpEndpointController {
    
    private final ContextService contextService;
    private final ContextWindowService windowService;
    private final ContextSharingService sharingService;
    private final ObjectMapper objectMapper;
    private final McpSecurityService mcpSecurityService;
    private final McpErrorHandler mcpErrorHandler;
    
    @PostMapping("/create_context")
    @Operation(summary = "Create context (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    @McpRateLimit(operationType = McpRateLimit.OperationType.WRITE, limitForPeriod = 20, limitRefreshPeriodSeconds = 60)
    public ResponseEntity<Map<String, Object>> createContext(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization and user from authenticated context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            UUID userId = mcpSecurityService.getAuthenticatedUserId(authentication);
            
            // Validate required parameters
            mcpSecurityService.validateRequiredParameter(params.get("name"), "name");
            
            CreateContextRequest request = CreateContextRequest.builder()
                    .organizationId(organizationId)
                    .userId(userId)
                    .name((String) params.get("name"))
                    .description((String) params.get("description"))
                    .build();
            
            ContextDto context = contextService.createContext(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("context", context);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "create_context", null);
        }
    }
    
    @PostMapping("/append_message")
    @Operation(summary = "Append message to context (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    @McpRateLimit(operationType = McpRateLimit.OperationType.WRITE, limitForPeriod = 100, limitRefreshPeriodSeconds = 60)
    public ResponseEntity<Map<String, Object>> appendMessage(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization from authenticated context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            UUID contextId = mcpSecurityService.validateUuidParameter(params.get("contextId"), "contextId");
            
            // Validate required parameters
            mcpSecurityService.validateRequiredParameter(params.get("role"), "role");
            mcpSecurityService.validateRequiredParameter(params.get("content"), "content");
            
            AppendMessageRequest request = AppendMessageRequest.builder()
                    .role((String) params.get("role"))
                    .content((String) params.get("content"))
                    .build();
            
            MessageDto message = contextService.appendMessage(contextId, organizationId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "append_message", null);
        }
    }
    
    @PostMapping("/get_context_window")
    @Operation(summary = "Get context window (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    @McpRateLimit(operationType = McpRateLimit.OperationType.EXPENSIVE, limitForPeriod = 30, limitRefreshPeriodSeconds = 60)
    public ResponseEntity<Map<String, Object>> getContextWindow(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization from authenticated context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            UUID contextId = mcpSecurityService.validateUuidParameter(params.get("contextId"), "contextId");
            
            ContextWindowRequest request = ContextWindowRequest.builder()
                    .maxTokens(params.containsKey("maxTokens") ? (Integer) params.get("maxTokens") : 4096)
                    .messageLimit(params.containsKey("messageLimit") ? (Integer) params.get("messageLimit") : null)
                    .includeSystemMessages(params.containsKey("includeSystemMessages") ? 
                            (Boolean) params.get("includeSystemMessages") : true)
                    .preserveMessageBoundaries(params.containsKey("preserveMessageBoundaries") ? 
                            (Boolean) params.get("preserveMessageBoundaries") : true)
                    .build();
            
            ContextWindowResponse window = windowService.getContextWindow(contextId, organizationId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("window", window);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "get_context_window", null);
        }
    }
    
    @PostMapping("/search_contexts")
    @Operation(summary = "Search contexts (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    @McpRateLimit(operationType = McpRateLimit.OperationType.READ, limitForPeriod = 50, limitRefreshPeriodSeconds = 60)
    public ResponseEntity<Map<String, Object>> searchContexts(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization from authenticated context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            
            // Validate required parameters
            mcpSecurityService.validateRequiredParameter(params.get("query"), "query");
            String query = (String) params.get("query");
            
            int page = params.containsKey("page") ? (Integer) params.get("page") : 0;
            int size = params.containsKey("size") ? (Integer) params.get("size") : 20;
            
            Page<ContextDto> contexts = contextService.searchContexts(
                    organizationId, query, PageRequest.of(page, size));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("contexts", contexts.getContent());
            response.put("totalElements", contexts.getTotalElements());
            response.put("totalPages", contexts.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "search_contexts", null);
        }
    }
    
    @PostMapping("/share_context")
    @Operation(summary = "Share context (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    @McpRateLimit(operationType = McpRateLimit.OperationType.WRITE, limitForPeriod = 10, limitRefreshPeriodSeconds = 60)
    public ResponseEntity<Map<String, Object>> shareContext(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization and user from authenticated context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            UUID sharedBy = mcpSecurityService.getAuthenticatedUserId(authentication);
            UUID contextId = mcpSecurityService.validateUuidParameter(params.get("contextId"), "contextId");
            
            // Validate required parameters
            mcpSecurityService.validateRequiredParameter(params.get("permission"), "permission");
            
            ShareContextRequest request = ShareContextRequest.builder()
                    .targetOrganizationId(params.containsKey("targetOrganizationId") ? 
                            mcpSecurityService.validateUuidParameter(params.get("targetOrganizationId"), "targetOrganizationId") : null)
                    .targetUserId(params.containsKey("targetUserId") ? 
                            mcpSecurityService.validateUuidParameter(params.get("targetUserId"), "targetUserId") : null)
                    .permission((String) params.get("permission"))
                    .build();
            
            SharedContext share = sharingService.shareContext(contextId, organizationId, sharedBy, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("share", share);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "share_context", null);
        }
    }
}