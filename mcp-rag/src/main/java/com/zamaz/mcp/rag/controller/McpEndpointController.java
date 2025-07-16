package com.zamaz.mcp.rag.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.rag.dto.*;
import com.zamaz.mcp.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

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
    private final DocumentService documentService;

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
        
        // Generate RAG context tool
        ObjectNode generateContext = tools.addObject();
        generateContext.put("name", "generate_rag_context");
        generateContext.put("description", "Generate context from documents for a query");
        ObjectNode contextParams = generateContext.putObject("parameters");
        contextParams.put("type", "object");
        ObjectNode contextProps = contextParams.putObject("properties");
        contextProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        contextProps.putObject("query").put("type", "string").put("description", "Query for context generation");
        contextProps.putObject("maxTokens").put("type", "integer").put("description", "Maximum tokens in context");
        contextProps.putObject("includeMetadata").put("type", "boolean").put("description", "Include document metadata");
        contextParams.putArray("required").add("organizationId").add("query");
        
        // List documents tool
        ObjectNode listDocuments = tools.addObject();
        listDocuments.put("name", "list_documents");
        listDocuments.put("description", "List stored documents for an organization");
        ObjectNode listParams = listDocuments.putObject("parameters");
        listParams.put("type", "object");
        ObjectNode listProps = listParams.putObject("properties");
        listProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        listProps.putObject("page").put("type", "integer").put("description", "Page number (0-based)");
        listProps.putObject("size").put("type", "integer").put("description", "Page size");
        listParams.putArray("required").add("organizationId");
        
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
                
            case "generate_rag_context":
                return generateRagContext(params);
                
            case "list_documents":
                return listDocuments(params);
                
            default:
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Unknown tool: " + toolName);
                return Mono.just(error);
        }
    }

    private Mono<JsonNode> storeDocument(JsonNode params) {
        try {
            String organizationId = params.get("organizationId").asText();
            String documentId = params.get("documentId").asText();
            String content = params.get("content").asText();
            String title = params.has("title") ? params.get("title").asText() : documentId;
            
            Map<String, String> metadata = new HashMap<>();
            if (params.has("metadata") && params.get("metadata").isObject()) {
                params.get("metadata").fields().forEachRemaining(entry -> {
                    metadata.put(entry.getKey(), entry.getValue().asText());
                });
            }
            
            DocumentRequest request = DocumentRequest.builder()
                    .organizationId(organizationId)
                    .documentId(documentId)
                    .title(title)
                    .content(content)
                    .metadata(metadata)
                    .build();
            
            DocumentResponse docResponse = documentService.storeDocument(request);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("documentId", docResponse.getDocumentId());
            response.put("status", docResponse.getStatus().toString());
            response.put("message", "Document stored successfully");
            
            return Mono.just(response);
            
        } catch (Exception e) {
            log.error("Error storing document", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Mono.just(error);
        }
    }

    private Mono<JsonNode> searchDocuments(JsonNode params) {
        try {
            String organizationId = params.get("organizationId").asText();
            String query = params.get("query").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 10;
            
            SearchRequest request = SearchRequest.builder()
                    .organizationId(organizationId)
                    .query(query)
                    .limit(limit)
                    .includeContent(true)
                    .build();
            
            SearchResponse searchResponse = documentService.searchDocuments(request);
            
            ObjectNode response = objectMapper.createObjectNode();
            ArrayNode results = response.putArray("results");
            
            for (SearchResult result : searchResponse.getResults()) {
                ObjectNode resultNode = results.addObject();
                resultNode.put("documentId", result.getDocumentId());
                resultNode.put("chunkId", result.getChunkId());
                resultNode.put("score", result.getScore());
                resultNode.put("content", result.getContent());
                if (result.getTitle() != null) {
                    resultNode.put("title", result.getTitle());
                }
            }
            
            response.put("query", searchResponse.getQuery());
            response.put("count", searchResponse.getTotalResults());
            response.put("searchTimeMs", searchResponse.getSearchTimeMs());
            
            return Mono.just(response);
            
        } catch (Exception e) {
            log.error("Error searching documents", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", e.getMessage());
            return Mono.just(error);
        }
    }

    private Mono<JsonNode> deleteDocument(JsonNode params) {
        try {
            String organizationId = params.get("organizationId").asText();
            String documentId = params.get("documentId").asText();
            
            documentService.deleteDocument(organizationId, documentId);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("documentId", documentId);
            response.put("message", "Document deleted successfully");
            
            return Mono.just(response);
            
        } catch (Exception e) {
            log.error("Error deleting document", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Mono.just(error);
        }
    }

    private Mono<JsonNode> generateRagContext(JsonNode params) {
        try {
            String organizationId = params.get("organizationId").asText();
            String query = params.get("query").asText();
            int maxTokens = params.has("maxTokens") ? params.get("maxTokens").asInt() : 2000;
            boolean includeMetadata = params.has("includeMetadata") && params.get("includeMetadata").asBoolean();
            
            // Search for relevant documents
            SearchRequest request = SearchRequest.builder()
                    .organizationId(organizationId)
                    .query(query)
                    .limit(5) // Get top 5 chunks
                    .includeContent(true)
                    .includeMetadata(includeMetadata)
                    .build();
            
            SearchResponse searchResponse = documentService.searchDocuments(request);
            
            // Build context from search results
            ObjectNode response = objectMapper.createObjectNode();
            StringBuilder contextBuilder = new StringBuilder();
            ArrayNode sources = response.putArray("sources");
            
            int currentTokens = 0;
            for (SearchResult result : searchResponse.getResults()) {
                String content = result.getContent();
                int estimatedTokens = content.length() / 4; // Rough estimate
                
                if (currentTokens + estimatedTokens > maxTokens) {
                    break;
                }
                
                if (contextBuilder.length() > 0) {
                    contextBuilder.append("\n\n---\n\n");
                }
                
                contextBuilder.append("Source: ").append(result.getTitle() != null ? result.getTitle() : result.getDocumentId());
                contextBuilder.append(" (Score: ").append(String.format("%.2f", result.getScore())).append(")\n\n");
                contextBuilder.append(content);
                
                ObjectNode source = sources.addObject();
                source.put("documentId", result.getDocumentId());
                source.put("chunkId", result.getChunkId());
                source.put("score", result.getScore());
                if (result.getTitle() != null) {
                    source.put("title", result.getTitle());
                }
                
                currentTokens += estimatedTokens;
            }
            
            response.put("context", contextBuilder.toString());
            response.put("query", query);
            response.put("sourceCount", sources.size());
            response.put("estimatedTokens", currentTokens);
            
            return Mono.just(response);
            
        } catch (Exception e) {
            log.error("Error generating RAG context", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", e.getMessage());
            return Mono.just(error);
        }
    }
    
    private Mono<JsonNode> listDocuments(JsonNode params) {
        try {
            String organizationId = params.get("organizationId").asText();
            int page = params.has("page") ? params.get("page").asInt() : 0;
            int size = params.has("size") ? params.get("size").asInt() : 20;
            
            Page<DocumentResponse> documents = documentService.listDocuments(
                    organizationId, 
                    org.springframework.data.domain.PageRequest.of(page, size)
            );
            
            ObjectNode response = objectMapper.createObjectNode();
            ArrayNode items = response.putArray("documents");
            
            for (DocumentResponse doc : documents.getContent()) {
                ObjectNode docNode = items.addObject();
                docNode.put("id", doc.getId());
                docNode.put("documentId", doc.getDocumentId());
                docNode.put("title", doc.getTitle());
                docNode.put("status", doc.getStatus().toString());
                docNode.put("chunkCount", doc.getChunkCount());
                docNode.put("createdAt", doc.getCreatedAt().toString());
                if (doc.getProcessedAt() != null) {
                    docNode.put("processedAt", doc.getProcessedAt().toString());
                }
            }
            
            response.put("totalElements", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("page", page);
            response.put("size", size);
            
            return Mono.just(response);
            
        } catch (Exception e) {
            log.error("Error listing documents", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", e.getMessage());
            return Mono.just(error);
        }
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}