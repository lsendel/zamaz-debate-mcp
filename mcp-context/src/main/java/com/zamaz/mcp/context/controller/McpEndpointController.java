package com.zamaz.mcp.context.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * MCP endpoint controller for the Context service.
 * Provides MCP protocol compatibility.
 */
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
        response.put("name", "mcp-context");
        response.put("version", "1.0.0");
        response.put("description", "Multi-tenant context management service for MCP system");
        
        ObjectNode capabilities = response.putObject("capabilities");
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        
        return Mono.just(response);
    }
    
    @PostMapping(value = "/list-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode tools = response.putArray("tools");
        
        // Create context tool
        ObjectNode createContext = tools.addObject();
        createContext.put("name", "create_context");
        createContext.put("description", "Create a new conversation context");
        ObjectNode createContextParams = createContext.putObject("parameters");
        createContextParams.put("type", "object");
        ObjectNode createContextProps = createContextParams.putObject("properties");
        createContextProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        createContextProps.putObject("userId").put("type", "string").put("description", "User ID");
        createContextProps.putObject("name").put("type", "string").put("description", "Context name");
        createContextProps.putObject("description").put("type", "string").put("description", "Context description");
        createContextParams.putArray("required").add("organizationId").add("userId").add("name");
        
        // Append message tool
        ObjectNode appendMessage = tools.addObject();
        appendMessage.put("name", "append_message");
        appendMessage.put("description", "Append a message to a context");
        ObjectNode appendMessageParams = appendMessage.putObject("parameters");
        appendMessageParams.put("type", "object");
        ObjectNode appendMessageProps = appendMessageParams.putObject("properties");
        appendMessageProps.putObject("contextId").put("type", "string").put("description", "Context ID");
        appendMessageProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        appendMessageProps.putObject("role").put("type", "string").put("description", "Message role (user/assistant/system)");
        appendMessageProps.putObject("content").put("type", "string").put("description", "Message content");
        appendMessageParams.putArray("required").add("contextId").add("organizationId").add("role").add("content");
        
        // Get context window tool
        ObjectNode getWindow = tools.addObject();
        getWindow.put("name", "get_context_window");
        getWindow.put("description", "Get a context window with token management");
        ObjectNode getWindowParams = getWindow.putObject("parameters");
        getWindowParams.put("type", "object");
        ObjectNode getWindowProps = getWindowParams.putObject("properties");
        getWindowProps.putObject("contextId").put("type", "string").put("description", "Context ID");
        getWindowProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        getWindowProps.putObject("maxTokens").put("type", "integer").put("description", "Maximum tokens in window");
        getWindowProps.putObject("messageLimit").put("type", "integer").put("description", "Maximum messages to return");
        getWindowParams.putArray("required").add("contextId").add("organizationId");
        
        // Search contexts tool
        ObjectNode searchContexts = tools.addObject();
        searchContexts.put("name", "search_contexts");
        searchContexts.put("description", "Search contexts by name or description");
        ObjectNode searchParams = searchContexts.putObject("parameters");
        searchParams.put("type", "object");
        ObjectNode searchProps = searchParams.putObject("properties");
        searchProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        searchProps.putObject("query").put("type", "string").put("description", "Search query");
        searchParams.putArray("required").add("organizationId").add("query");
        
        // Share context tool
        ObjectNode shareContext = tools.addObject();
        shareContext.put("name", "share_context");
        shareContext.put("description", "Share a context with another organization or user");
        ObjectNode shareParams = shareContext.putObject("parameters");
        shareParams.put("type", "object");
        ObjectNode shareProps = shareParams.putObject("properties");
        shareProps.putObject("contextId").put("type", "string").put("description", "Context ID");
        shareProps.putObject("organizationId").put("type", "string").put("description", "Source organization ID");
        shareProps.putObject("targetOrganizationId").put("type", "string").put("description", "Target organization ID");
        shareProps.putObject("targetUserId").put("type", "string").put("description", "Target user ID");
        shareProps.putObject("permission").put("type", "string").put("description", "Permission level (READ/WRITE/ADMIN)");
        shareParams.putArray("required").add("contextId").add("organizationId").add("permission");
        
        return Mono.just(response);
    }
    
    @PostMapping(value = "/call-tool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> callTool(@RequestBody JsonNode request) {
        String toolName = request.get("name").asText();
        JsonNode params = request.get("arguments");
        
        log.info("MCP tool call: {} with params: {}", toolName, params);
        
        // Delegate to the McpToolsController
        return mcpToolsController.callTool(toolName, params)
            .map(result -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.set("result", result);
                return (JsonNode) response;
            })
            .onErrorResume(error -> {
                log.error("Error calling tool: {}", toolName, error);
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