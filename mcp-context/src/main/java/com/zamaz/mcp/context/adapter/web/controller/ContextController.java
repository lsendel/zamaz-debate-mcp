package com.zamaz.mcp.context.adapter.web.controller;

import com.zamaz.mcp.common.architecture.adapter.web.WebAdapter;
import com.zamaz.mcp.context.adapter.web.dto.*;
import com.zamaz.mcp.context.adapter.web.mapper.ContextWebMapper;
import com.zamaz.mcp.context.application.command.ArchiveContextCommand;
import com.zamaz.mcp.context.application.command.DeleteContextCommand;
import com.zamaz.mcp.context.application.port.inbound.*;
import com.zamaz.mcp.context.application.query.GetContextQuery;
import com.zamaz.mcp.context.application.query.GetContextWindowQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for context management.
 * This is a web adapter in hexagonal architecture.
 */
@RestController
@RequestMapping("/api/v1/contexts")
@Tag(name = "Context Management", description = "API for managing conversation contexts")
public class ContextController implements WebAdapter {
    
    private final CreateContextUseCase createContextUseCase;
    private final GetContextUseCase getContextUseCase;
    private final AppendMessageUseCase appendMessageUseCase;
    private final GetContextWindowUseCase getContextWindowUseCase;
    private final UpdateContextMetadataUseCase updateMetadataUseCase;
    private final ArchiveContextUseCase archiveContextUseCase;
    private final DeleteContextUseCase deleteContextUseCase;
    private final ContextWebMapper mapper;
    
    public ContextController(
            CreateContextUseCase createContextUseCase,
            GetContextUseCase getContextUseCase,
            AppendMessageUseCase appendMessageUseCase,
            GetContextWindowUseCase getContextWindowUseCase,
            UpdateContextMetadataUseCase updateMetadataUseCase,
            ArchiveContextUseCase archiveContextUseCase,
            DeleteContextUseCase deleteContextUseCase,
            ContextWebMapper mapper
    ) {
        this.createContextUseCase = createContextUseCase;
        this.getContextUseCase = getContextUseCase;
        this.appendMessageUseCase = appendMessageUseCase;
        this.getContextWindowUseCase = getContextWindowUseCase;
        this.updateMetadataUseCase = updateMetadataUseCase;
        this.archiveContextUseCase = archiveContextUseCase;
        this.deleteContextUseCase = deleteContextUseCase;
        this.mapper = mapper;
    }
    
    @PostMapping
    @Operation(summary = "Create a new context")
    public ResponseEntity<ContextResponse> createContext(
            @Valid @RequestBody CreateContextRequest request,
            @RequestHeader("X-Organization-Id") String organizationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var command = mapper.toCommand(request, organizationId, userDetails.getUsername());
        var contextId = createContextUseCase.execute(command);
        
        // Retrieve the created context
        var query = new GetContextQuery(contextId.asString(), organizationId);
        var contextView = getContextUseCase.execute(query);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapper.toResponse(contextView));
    }
    
    @GetMapping("/{contextId}")
    @Operation(summary = "Get a context by ID")
    public ResponseEntity<ContextResponse> getContext(
            @PathVariable String contextId,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var query = new GetContextQuery(contextId, organizationId);
        var contextView = getContextUseCase.execute(query);
        
        return ResponseEntity.ok(mapper.toResponse(contextView));
    }
    
    @PostMapping("/{contextId}/messages")
    @Operation(summary = "Append a message to a context")
    public ResponseEntity<Void> appendMessage(
            @PathVariable String contextId,
            @Valid @RequestBody AppendMessageRequest request,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var command = mapper.toCommand(request, contextId, organizationId);
        appendMessageUseCase.execute(command);
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("/{contextId}/window")
    @Operation(summary = "Get a context window with token limits")
    public ResponseEntity<ContextWindowResponse> getContextWindow(
            @PathVariable String contextId,
            @RequestParam @Min(1) int maxTokens,
            @RequestParam(required = false) @Min(1) Integer maxMessages,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var query = GetContextWindowQuery.of(contextId, organizationId, maxTokens, maxMessages);
        var window = getContextWindowUseCase.execute(query);
        
        return ResponseEntity.ok(mapper.toResponse(window));
    }
    
    @PatchMapping("/{contextId}/metadata")
    @Operation(summary = "Update context metadata")
    public ResponseEntity<Void> updateMetadata(
            @PathVariable String contextId,
            @Valid @RequestBody UpdateMetadataRequest request,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var command = mapper.toCommand(request, contextId, organizationId);
        updateMetadataUseCase.execute(command);
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{contextId}/archive")
    @Operation(summary = "Archive a context")
    public ResponseEntity<Void> archiveContext(
            @PathVariable String contextId,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var command = new ArchiveContextCommand(contextId, organizationId);
        archiveContextUseCase.execute(command);
        
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{contextId}")
    @Operation(summary = "Delete a context")
    public ResponseEntity<Void> deleteContext(
            @PathVariable String contextId,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var command = new DeleteContextCommand(contextId, organizationId);
        deleteContextUseCase.execute(command);
        
        return ResponseEntity.noContent().build();
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            IllegalArgumentException ex,
            @RequestAttribute("jakarta.servlet.error.request_uri") String path
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.validation(ex.getMessage(), path));
    }
    
    @ExceptionHandler(com.zamaz.mcp.common.application.exception.ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            com.zamaz.mcp.common.application.exception.ResourceNotFoundException ex,
            @RequestAttribute("jakarta.servlet.error.request_uri") String path
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.notFound(ex.getMessage(), path));
    }
}