package com.zamaz.mcp.organization.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpEndpointController {

    private final ObjectMapper objectMapper;
    private final McpToolsController mcpToolsController;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getServerInfo() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("name", "mcp-organization");
        response.put("version", "1.0.0");
        response.put("description", "Organization management service for multi-tenant debate system");
        
        ObjectNode capabilities = response.putObject("capabilities");
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        
        return Mono.just(response);
    }

    @PostMapping(value = "/list-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode tools = response.putArray("tools");
        
        // Organization tools
        ObjectNode createOrg = tools.addObject();
        createOrg.put("name", "create_organization");
        createOrg.put("description", "Create a new organization");
        ObjectNode createOrgParams = createOrg.putObject("parameters");
        createOrgParams.put("type", "object");
        ObjectNode createOrgProps = createOrgParams.putObject("properties");
        createOrgProps.putObject("name").put("type", "string").put("description", "Organization name");
        createOrgProps.putObject("description").put("type", "string").put("description", "Organization description");
        createOrgParams.putArray("required").add("name");
        
        // List organizations tool
        ObjectNode listOrgs = tools.addObject();
        listOrgs.put("name", "list_organizations");
        listOrgs.put("description", "List all organizations");
        listOrgs.putObject("parameters").put("type", "object");
        
        // Create user tool
        ObjectNode createUser = tools.addObject();
        createUser.put("name", "create_user");
        createUser.put("description", "Create a new user");
        ObjectNode createUserParams = createUser.putObject("parameters");
        createUserParams.put("type", "object");
        ObjectNode createUserProps = createUserParams.putObject("properties");
        createUserProps.putObject("email").put("type", "string").put("description", "User email");
        createUserProps.putObject("name").put("type", "string").put("description", "User name");
        createUserProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        createUserParams.putArray("required").add("email").add("name").add("organizationId");
        
        return Mono.just(response);
    }

    @PostMapping(value = "/call-tool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> callTool(@RequestBody JsonNode request) {
        String toolName = request.get("name").asText();
        JsonNode params = request.get("arguments");
        
        log.info("MCP tool call: {} with params: {}", toolName, params);
        
        // Delegate to the existing McpToolsController
        return mcpToolsController.callTool(toolName, params)
            .map(result -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.set("result", result);
                return (JsonNode) response;
            })
            .onErrorResume(error -> {
                ObjectNode errorResponse = objectMapper.createObjectNode();
                errorResponse.put("error", error.getMessage());
                return Mono.just((JsonNode) errorResponse);
            });
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}