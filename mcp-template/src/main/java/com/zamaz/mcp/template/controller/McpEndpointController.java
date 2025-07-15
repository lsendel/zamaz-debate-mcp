package com.zamaz.mcp.template.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * MCP Protocol endpoint controller for Template service.
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpEndpointController {

    private final ObjectMapper objectMapper;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getServerInfo() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("name", "mcp-template");
        response.put("version", "1.0.0");
        response.put("description", "Template management service for MCP");
        
        ObjectNode capabilities = response.putObject("capabilities");
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        
        return Mono.just(response);
    }

    @PostMapping(value = "/list-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode tools = response.putArray("tools");
        
        // Create template tool
        ObjectNode createTemplate = tools.addObject();
        createTemplate.put("name", "create_template");
        createTemplate.put("description", "Create a new template");
        ObjectNode createParams = createTemplate.putObject("parameters");
        createParams.put("type", "object");
        ObjectNode createProps = createParams.putObject("properties");
        createProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        createProps.putObject("name").put("type", "string").put("description", "Template name");
        createProps.putObject("description").put("type", "string").put("description", "Template description");
        createProps.putObject("category").put("type", "string").put("description", "Template category");
        createProps.putObject("content").put("type", "string").put("description", "Template content");
        createProps.putObject("variables").put("type", "array").put("description", "Template variables");
        createParams.putArray("required").add("organizationId").add("name").add("content");
        
        // Get template tool
        ObjectNode getTemplate = tools.addObject();
        getTemplate.put("name", "get_template");
        getTemplate.put("description", "Get a template by ID");
        ObjectNode getParams = getTemplate.putObject("parameters");
        getParams.put("type", "object");
        ObjectNode getProps = getParams.putObject("properties");
        getProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        getProps.putObject("templateId").put("type", "string").put("description", "Template ID");
        getParams.putArray("required").add("organizationId").add("templateId");
        
        // List templates tool
        ObjectNode listTemplates = tools.addObject();
        listTemplates.put("name", "list_templates");
        listTemplates.put("description", "List templates for an organization");
        ObjectNode listParams = listTemplates.putObject("parameters");
        listParams.put("type", "object");
        ObjectNode listProps = listParams.putObject("properties");
        listProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        listProps.putObject("category").put("type", "string").put("description", "Filter by category");
        listProps.putObject("status").put("type", "string").put("description", "Filter by status");
        listParams.putArray("required").add("organizationId");
        
        // Render template tool
        ObjectNode renderTemplate = tools.addObject();
        renderTemplate.put("name", "render_template");
        renderTemplate.put("description", "Render a template with variables");
        ObjectNode renderParams = renderTemplate.putObject("parameters");
        renderParams.put("type", "object");
        ObjectNode renderProps = renderParams.putObject("properties");
        renderProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        renderProps.putObject("templateId").put("type", "string").put("description", "Template ID");
        renderProps.putObject("variables").put("type", "object").put("description", "Variables to render");
        renderParams.putArray("required").add("organizationId").add("templateId");
        
        return Mono.just(response);
    }

    @PostMapping(value = "/call-tool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> callTool(@RequestBody JsonNode request) {
        String toolName = request.get("name").asText();
        JsonNode params = request.get("arguments");
        
        log.info("MCP tool call: {} with params: {}", toolName, params);
        
        switch (toolName) {
            case "create_template":
                return createTemplate(params);
                
            case "get_template":
                return getTemplate(params);
                
            case "list_templates":
                return listTemplates(params);
                
            case "render_template":
                return renderTemplate(params);
                
            default:
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Unknown tool: " + toolName);
                return Mono.just(error);
        }
    }

    private Mono<JsonNode> createTemplate(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.put("templateId", "template-" + System.currentTimeMillis());
        response.put("name", params.get("name").asText());
        response.put("message", "Template created successfully (placeholder)");
        return Mono.just(response);
    }

    private Mono<JsonNode> getTemplate(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("templateId", params.get("templateId").asText());
        response.put("name", "Sample Template");
        response.put("content", "Hello {{name}}, welcome to {{organization}}!");
        response.put("category", "GREETING");
        response.put("status", "ACTIVE");
        return Mono.just(response);
    }

    private Mono<JsonNode> listTemplates(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode templates = response.putArray("templates");
        
        // Add mock template
        ObjectNode template = templates.addObject();
        template.put("templateId", "template-123");
        template.put("name", "Welcome Template");
        template.put("category", "GREETING");
        template.put("status", "ACTIVE");
        
        response.put("organizationId", params.get("organizationId").asText());
        response.put("count", 1);
        return Mono.just(response);
    }

    private Mono<JsonNode> renderTemplate(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.put("rendered", "Hello John, welcome to Acme Corp!");
        response.put("templateId", params.get("templateId").asText());
        return Mono.just(response);
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}