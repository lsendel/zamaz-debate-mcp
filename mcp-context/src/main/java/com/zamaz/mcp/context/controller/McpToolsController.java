package com.zamaz.mcp.context.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * MCP tools implementation for the Context service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpToolsController {
    
    private final ContextService contextService;
    private final ContextWindowService windowService;
    private final ContextSharingService sharingService;
    private final ObjectMapper objectMapper;
    
    public Mono<JsonNode> callTool(String toolName, JsonNode params) {
        return switch (toolName) {
            case "create_context" -> createContext(params);
            case "append_message" -> appendMessage(params);
            case "get_context_window" -> getContextWindow(params);
            case "search_contexts" -> searchContexts(params);
            case "share_context" -> shareContext(params);
            default -> Mono.error(new IllegalArgumentException("Unknown tool: " + toolName));
        };
    }
    
    private Mono<JsonNode> createContext(JsonNode params) {
        try {
            CreateContextRequest request = CreateContextRequest.builder()
                    .organizationId(UUID.fromString(params.get("organizationId").asText()))
                    .userId(UUID.fromString(params.get("userId").asText()))
                    .name(params.get("name").asText())
                    .description(params.has("description") ? params.get("description").asText() : null)
                    .build();
            
            ContextDto context = contextService.createContext(request);
            return Mono.just(objectMapper.valueToTree(context));
        } catch (Exception e) {
            log.error("Error creating context", e);
            return Mono.error(e);
        }
    }
    
    private Mono<JsonNode> appendMessage(JsonNode params) {
        try {
            UUID contextId = UUID.fromString(params.get("contextId").asText());
            UUID organizationId = UUID.fromString(params.get("organizationId").asText());
            
            AppendMessageRequest request = AppendMessageRequest.builder()
                    .role(params.get("role").asText())
                    .content(params.get("content").asText())
                    .build();
            
            MessageDto message = contextService.appendMessage(contextId, organizationId, request);
            return Mono.just(objectMapper.valueToTree(message));
        } catch (Exception e) {
            log.error("Error appending message", e);
            return Mono.error(e);
        }
    }
    
    private Mono<JsonNode> getContextWindow(JsonNode params) {
        try {
            UUID contextId = UUID.fromString(params.get("contextId").asText());
            UUID organizationId = UUID.fromString(params.get("organizationId").asText());
            
            ContextWindowRequest request = ContextWindowRequest.builder()
                    .maxTokens(params.has("maxTokens") ? params.get("maxTokens").asInt() : 4096)
                    .messageLimit(params.has("messageLimit") ? params.get("messageLimit").asInt() : null)
                    .includeSystemMessages(params.has("includeSystemMessages") ? 
                            params.get("includeSystemMessages").asBoolean() : true)
                    .preserveMessageBoundaries(params.has("preserveMessageBoundaries") ? 
                            params.get("preserveMessageBoundaries").asBoolean() : true)
                    .build();
            
            ContextWindowResponse window = windowService.getContextWindow(contextId, organizationId, request);
            return Mono.just(objectMapper.valueToTree(window));
        } catch (Exception e) {
            log.error("Error getting context window", e);
            return Mono.error(e);
        }
    }
    
    private Mono<JsonNode> searchContexts(JsonNode params) {
        try {
            UUID organizationId = UUID.fromString(params.get("organizationId").asText());
            String query = params.get("query").asText();
            
            Page<ContextDto> contexts = contextService.searchContexts(
                    organizationId, query, PageRequest.of(0, 20));
            
            return Mono.just(objectMapper.valueToTree(contexts.getContent()));
        } catch (Exception e) {
            log.error("Error searching contexts", e);
            return Mono.error(e);
        }
    }
    
    private Mono<JsonNode> shareContext(JsonNode params) {
        try {
            UUID contextId = UUID.fromString(params.get("contextId").asText());
            UUID organizationId = UUID.fromString(params.get("organizationId").asText());
            UUID sharedBy = UUID.fromString(params.get("sharedBy").asText());
            
            ShareContextRequest request = ShareContextRequest.builder()
                    .targetOrganizationId(params.has("targetOrganizationId") ? 
                            UUID.fromString(params.get("targetOrganizationId").asText()) : null)
                    .targetUserId(params.has("targetUserId") ? 
                            UUID.fromString(params.get("targetUserId").asText()) : null)
                    .permission(params.get("permission").asText())
                    .build();
            
            SharedContext share = sharingService.shareContext(contextId, organizationId, sharedBy, request);
            return Mono.just(objectMapper.valueToTree(share));
        } catch (Exception e) {
            log.error("Error sharing context", e);
            return Mono.error(e);
        }
    }
}