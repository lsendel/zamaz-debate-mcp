package com.zamaz.mcp.organization.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.organization.dto.OrganizationDto;
import com.zamaz.mcp.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    
    @PostMapping("/create_organization")
    @Operation(summary = "Create organization (MCP Tool)")
    public ResponseEntity<Map<String, Object>> createOrganization(@RequestBody Map<String, Object> params) {
        try {
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
            log.error("Error creating organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/get_organization")
    @Operation(summary = "Get organization by ID (MCP Tool)")
    public ResponseEntity<Map<String, Object>> getOrganization(@RequestBody Map<String, Object> params) {
        try {
            UUID organizationId = UUID.fromString((String) params.get("organizationId"));
            OrganizationDto organization = organizationService.getOrganization(organizationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("organization", organization);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/update_organization")
    @Operation(summary = "Update organization (MCP Tool)")
    public ResponseEntity<Map<String, Object>> updateOrganization(@RequestBody Map<String, Object> params) {
        try {
            UUID organizationId = UUID.fromString((String) params.get("organizationId"));
            
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
        } catch (Exception e) {
            log.error("Error updating organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/delete_organization")
    @Operation(summary = "Delete organization (MCP Tool)")
    public ResponseEntity<Map<String, Object>> deleteOrganization(@RequestBody Map<String, Object> params) {
        try {
            UUID organizationId = UUID.fromString((String) params.get("organizationId"));
            organizationService.deleteOrganization(organizationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/add_user_to_organization")
    @Operation(summary = "Add user to organization (MCP Tool)")
    public ResponseEntity<Map<String, Object>> addUserToOrganization(@RequestBody Map<String, Object> params) {
        try {
            UUID organizationId = UUID.fromString((String) params.get("organizationId"));
            UUID userId = UUID.fromString((String) params.get("userId"));
            String role = (String) params.getOrDefault("role", "member");
            
            organizationService.addUserToOrganization(organizationId, userId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User added to organization successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding user to organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/remove_user_from_organization")
    @Operation(summary = "Remove user from organization (MCP Tool)")
    public ResponseEntity<Map<String, Object>> removeUserFromOrganization(@RequestBody Map<String, Object> params) {
        try {
            UUID organizationId = UUID.fromString((String) params.get("organizationId"));
            UUID userId = UUID.fromString((String) params.get("userId"));
            
            organizationService.removeUserFromOrganization(organizationId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User removed from organization successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error removing user from organization: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/resources/organizations")
    @Operation(summary = "List organizations (MCP Resource)")
    public ResponseEntity<Map<String, Object>> listOrganizationsResource() {
        try {
            List<OrganizationDto> organizations = organizationService.listOrganizations(Pageable.unpaged()).getContent();
            
            Map<String, Object> response = new HashMap<>();
            response.put("organizations", organizations);
            response.put("count", organizations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing organizations: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
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