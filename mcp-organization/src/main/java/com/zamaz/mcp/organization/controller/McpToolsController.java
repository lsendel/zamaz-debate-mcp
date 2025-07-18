package com.zamaz.mcp.organization.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.security.McpSecurityService;
import com.zamaz.mcp.common.security.McpSecurityException;
import com.zamaz.mcp.common.error.McpErrorHandler;
import com.zamaz.mcp.common.error.McpErrorResponse;
import com.zamaz.mcp.organization.dto.OrganizationDto;
import com.zamaz.mcp.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MCP Tools", description = "MCP protocol tool endpoints")
public class McpToolsController {
    
    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper;
    private final McpSecurityService mcpSecurityService;
    private final McpErrorHandler mcpErrorHandler;
    
    @PostMapping("/create_organization")
    @Operation(summary = "Create organization (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createOrganization(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Validate required parameters
            mcpSecurityService.validateRequiredParameter(params.get("name"), "name");
            
            OrganizationDto.CreateOrganizationRequest request = OrganizationDto.CreateOrganizationRequest.builder()
                    .name((String) params.get("name"))
                    .description((String) params.get("description"))
                    .build();
            
            OrganizationDto organization = organizationService.createOrganization(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("organization", organization);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "create_organization", null);
        }
    }
    
    @PostMapping("/get_organization")
    @Operation(summary = "Get organization by ID (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getOrganization(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization ID from authenticated user context instead of trusting client
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            
            OrganizationDto organization = organizationService.getOrganization(organizationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("organization", organization);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return mcpErrorHandler.createErrorResponse(e, "get_organization", null);
        }
    }
    
    @PostMapping("/update_organization")
    @Operation(summary = "Update organization (MCP Tool)")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateOrganization(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization ID from authenticated user context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            
            OrganizationDto.UpdateOrganizationRequest request = OrganizationDto.UpdateOrganizationRequest.builder()
                    .name((String) params.get("name"))
                    .description((String) params.get("description"))
                    .isActive((Boolean) params.get("isActive"))
                    .build();
            
            OrganizationDto organization = organizationService.updateOrganization(organizationId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("organization", organization);
            return ResponseEntity.ok(response);
        } catch (McpSecurityException e) {
            log.warn("Security error in update_organization: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Access denied");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/delete_organization")
    @Operation(summary = "Delete organization (MCP Tool)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteOrganization(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization ID from authenticated user context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            organizationService.deleteOrganization(organizationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization deleted successfully");
            return ResponseEntity.ok(response);
        } catch (McpSecurityException e) {
            log.warn("Security error in delete_organization: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Access denied");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            log.error("Error deleting organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/add_user_to_organization")
    @Operation(summary = "Add user to organization (MCP Tool)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> addUserToOrganization(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization ID from authenticated user context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            UUID userId = mcpSecurityService.validateUuidParameter(params.get("userId"), "userId");
            String role = (String) params.getOrDefault("role", "member");
            
            organizationService.addUserToOrganization(organizationId, userId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User added to organization successfully");
            return ResponseEntity.ok(response);
        } catch (McpSecurityException e) {
            log.warn("Security error in add_user_to_organization: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Access denied");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            log.error("Error adding user to organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/remove_user_from_organization")
    @Operation(summary = "Remove user from organization (MCP Tool)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeUserFromOrganization(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        try {
            // Extract organization ID from authenticated user context
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            UUID userId = mcpSecurityService.validateUuidParameter(params.get("userId"), "userId");
            
            organizationService.removeUserFromOrganization(organizationId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User removed from organization successfully");
            return ResponseEntity.ok(response);
        } catch (McpSecurityException e) {
            log.warn("Security error in remove_user_from_organization: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Access denied");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            log.error("Error removing user from organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/resources/organizations")
    @Operation(summary = "List organizations (MCP Resource)")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listOrganizationsResource(Authentication authentication) {
        try {
            // Only return the user's organization for security
            UUID organizationId = mcpSecurityService.getAuthenticatedOrganizationId(authentication);
            OrganizationDto organization = organizationService.getOrganization(organizationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("organizations", List.of(organization));
            response.put("count", 1);
            return ResponseEntity.ok(response);
        } catch (McpSecurityException e) {
            log.warn("Security error in list_organizations: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Access denied");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            log.error("Error listing organizations: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Generic tool call handler for MCP protocol
     */
    public Mono<JsonNode> callTool(String toolName, JsonNode params) {
        Map<String, Object> paramsMap = objectMapper.convertValue(params, Map.class);
        
        ResponseEntity<Map<String, Object>> response;
        switch (toolName) {
            case "create_organization":
                response = createOrganization(paramsMap);
                break;
            case "get_organization":
                response = getOrganization(paramsMap);
                break;
            case "update_organization":
                response = updateOrganization(paramsMap);
                break;
            case "delete_organization":
                response = deleteOrganization(paramsMap);
                break;
            case "add_user_to_organization":
                response = addUserToOrganization(paramsMap);
                break;
            case "remove_user_from_organization":
                response = removeUserFromOrganization(paramsMap);
                break;
            case "list_organizations":
                response = listOrganizationsResource();
                break;
            default:
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Unknown tool: " + toolName);
                response = ResponseEntity.badRequest().body(errorResponse);
        }
        
        return Mono.just(objectMapper.valueToTree(response.getBody()));
    }
}