package com.zamaz.mcp.rag.controller;

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
 * MCP Protocol endpoint controller for RAG service.
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
        response.put("name", "mcp-rag");
        response.put("version", "1.0.0");
        response.put("description", "Retrieval Augmented Generation service for MCP");
        
        ObjectNode capabilities = response.putObject("capabilities");
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        
        return Mono.just(response);
    }

    @PostMapping(value = "/list-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode tools = response.putArray("tools");
        
        // Store document tool
        ObjectNode storeDocument = tools.addObject();
        storeDocument.put("name", "store_document");
        storeDocument.put("description", "Store a document for retrieval");
        ObjectNode storeParams = storeDocument.putObject("parameters");
        storeParams.put("type", "object");
        ObjectNode storeProps = storeParams.putObject("properties");
        storeProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        storeProps.putObject("documentId").put("type", "string").put("description", "Document ID");
        storeProps.putObject("content").put("type", "string").put("description", "Document content");
        storeProps.putObject("metadata").put("type", "object").put("description", "Document metadata");
        storeParams.putArray("required").add("organizationId").add("documentId").add("content");
        
        // Search documents tool
        ObjectNode searchDocuments = tools.addObject();
        searchDocuments.put("name", "search_documents");
        searchDocuments.put("description", "Search for relevant documents");
        ObjectNode searchParams = searchDocuments.putObject("parameters");
        searchParams.put("type", "object");
        ObjectNode searchProps = searchParams.putObject("properties");
        searchProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        searchProps.putObject("query").put("type", "string").put("description", "Search query");
        searchProps.putObject("limit").put("type", "integer").put("description", "Number of results");
        searchParams.putArray("required").add("organizationId").add("query");
        
        // Delete document tool
        ObjectNode deleteDocument = tools.addObject();
        deleteDocument.put("name", "delete_document");
        deleteDocument.put("description", "Delete a stored document");
        ObjectNode deleteParams = deleteDocument.putObject("parameters");
        deleteParams.put("type", "object");
        ObjectNode deleteProps = deleteParams.putObject("properties");
        deleteProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        deleteProps.putObject("documentId").put("type", "string").put("description", "Document ID");
        deleteParams.putArray("required").add("organizationId").add("documentId");
        
        return Mono.just(response);
    }

    @PostMapping(value = "/call-tool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> callTool(@RequestBody JsonNode request) {
        String toolName = request.get("name").asText();
        JsonNode params = request.get("arguments");
        
        log.info("MCP tool call: {} with params: {}", toolName, params);
        
        switch (toolName) {
            case "store_document":
                return storeDocument(params);
                
            case "search_documents":
                return searchDocuments(params);
                
            case "delete_document":
                return deleteDocument(params);
                
            default:
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Unknown tool: " + toolName);
                return Mono.just(error);
        }
    }

    private Mono<JsonNode> storeDocument(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.put("documentId", params.get("documentId").asText());
        response.put("message", "Document stored successfully (placeholder)");
        return Mono.just(response);
    }

    private Mono<JsonNode> searchDocuments(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode results = response.putArray("results");
        
        // Add mock result
        ObjectNode result = results.addObject();
        result.put("documentId", "doc-123");
        result.put("score", 0.95);
        result.put("content", "This is a placeholder search result");
        
        response.put("query", params.get("query").asText());
        response.put("count", 1);
        return Mono.just(response);
    }

    private Mono<JsonNode> deleteDocument(JsonNode params) {
        // Placeholder implementation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.put("documentId", params.get("documentId").asText());
        response.put("message", "Document deleted successfully (placeholder)");
        return Mono.just(response);
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}