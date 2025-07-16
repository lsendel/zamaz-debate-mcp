package com.zamaz.mcp.context.controller;

import com.zamaz.mcp.common.dto.ApiResponse;
import com.zamaz.mcp.context.dto.AppendMessageRequest;
import com.zamaz.mcp.context.dto.ContextDto;
import com.zamaz.mcp.context.dto.ContextWindowRequest;
import com.zamaz.mcp.context.dto.ContextWindowResponse;
import com.zamaz.mcp.context.dto.CreateContextRequest;
import com.zamaz.mcp.context.dto.MessageDto;
import com.zamaz.mcp.context.service.ContextService;
import com.zamaz.mcp.context.service.ContextWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for context management operations.
 */
@RestController
@RequestMapping("/api/contexts")
@RequiredArgsConstructor
@Tag(name = "Context Management", description = "APIs for managing conversation contexts")
public class ContextController {
    
    private final ContextService contextService;
    private final ContextWindowService windowService;
    
    @PostMapping
    @Operation(summary = "Create a new context")
    public ResponseEntity<ApiResponse<ContextDto>> createContext(@Valid @RequestBody CreateContextRequest request) {
        ContextDto context = contextService.createContext(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(context, "Context created successfully"));
    }
    
    @GetMapping("/{contextId}")
    @Operation(summary = "Get a context by ID")
    public ResponseEntity<ApiResponse<ContextDto>> getContext(
            @PathVariable UUID contextId,
            @RequestHeader("X-Organization-Id") UUID organizationId) {
        ContextDto context = contextService.getContext(contextId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(context));
    }
    
    @GetMapping
    @Operation(summary = "List contexts for an organization")
    public ResponseEntity<ApiResponse<Page<ContextDto>>> listContexts(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ContextDto> contexts = contextService.listContexts(organizationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(contexts));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search contexts")
    public ResponseEntity<ApiResponse<Page<ContextDto>>> searchContexts(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ContextDto> contexts = contextService.searchContexts(organizationId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(contexts));
    }
    
    @PostMapping("/{contextId}/messages")
    @Operation(summary = "Append a message to a context")
    public ResponseEntity<ApiResponse<MessageDto>> appendMessage(
            @PathVariable UUID contextId,
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @Valid @RequestBody AppendMessageRequest request) {
        MessageDto message = contextService.appendMessage(contextId, organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, "Message appended successfully"));
    }
    
    @GetMapping("/{contextId}/messages")
    @Operation(summary = "Get messages for a context")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
            @PathVariable UUID contextId,
            @RequestHeader("X-Organization-Id") UUID organizationId) {
        List<MessageDto> messages = contextService.getMessages(contextId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    @PostMapping("/{contextId}/window")
    @Operation(summary = "Get a context window with token management")
    public ResponseEntity<ApiResponse<ContextWindowResponse>> getContextWindow(
            @PathVariable UUID contextId,
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @Valid @RequestBody ContextWindowRequest request) {
        ContextWindowResponse window = windowService.getContextWindow(contextId, organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(window));
    }
    
    @DeleteMapping("/{contextId}")
    @Operation(summary = "Delete a context")
    public ResponseEntity<ApiResponse<Void>> deleteContext(
            @PathVariable UUID contextId,
            @RequestHeader("X-Organization-Id") UUID organizationId) {
        contextService.deleteContext(contextId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Context deleted successfully"));
    }
    
    @PostMapping("/{contextId}/archive")
    @Operation(summary = "Archive a context")
    public ResponseEntity<ApiResponse<Void>> archiveContext(
            @PathVariable UUID contextId,
            @RequestHeader("X-Organization-Id") UUID organizationId) {
        contextService.archiveContext(contextId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Context archived successfully"));
    }
}