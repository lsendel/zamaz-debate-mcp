package com.zamaz.mcp.llm.adapter.web.controller;

import com.zamaz.mcp.common.architecture.adapter.web.WebAdapter;
import com.zamaz.mcp.llm.adapter.web.dto.*;
import com.zamaz.mcp.llm.adapter.web.mapper.LlmWebMapper;
import com.zamaz.mcp.llm.application.command.CheckProviderHealthCommand;
import com.zamaz.mcp.llm.application.port.inbound.*;
import com.zamaz.mcp.llm.application.query.ListProvidersQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller for LLM completion operations.
 * This is a web adapter in hexagonal architecture.
 */
@RestController
@RequestMapping("/api/v1/llm")
@Tag(name = "LLM Completion", description = "API for generating text completions using various LLM providers")
public class CompletionController implements WebAdapter {
    
    private final GenerateCompletionUseCase generateCompletionUseCase;
    private final StreamCompletionUseCase streamCompletionUseCase;
    private final ListProvidersUseCase listProvidersUseCase;
    private final CheckProviderHealthUseCase checkProviderHealthUseCase;
    private final LlmWebMapper mapper;
    
    public CompletionController(
            GenerateCompletionUseCase generateCompletionUseCase,
            StreamCompletionUseCase streamCompletionUseCase,
            ListProvidersUseCase listProvidersUseCase,
            CheckProviderHealthUseCase checkProviderHealthUseCase,
            LlmWebMapper mapper
    ) {
        this.generateCompletionUseCase = generateCompletionUseCase;
        this.streamCompletionUseCase = streamCompletionUseCase;
        this.listProvidersUseCase = listProvidersUseCase;
        this.checkProviderHealthUseCase = checkProviderHealthUseCase;
        this.mapper = mapper;
    }
    
    @PostMapping("/completions")
    @Operation(summary = "Generate a text completion")
    public ResponseEntity<CompletionResponse> generateCompletion(
            @Valid @RequestBody CompletionRequest request,
            @RequestHeader("X-Organization-Id") String organizationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (request.isStreaming()) {
            throw new IllegalArgumentException("Use streaming endpoint for streaming completions");
        }
        
        var command = mapper.toGenerateCommand(request, organizationId, userDetails.getUsername());
        var result = generateCompletionUseCase.execute(command);
        
        return ResponseEntity.ok(mapper.toResponse(result));
    }
    
    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Generate a streaming text completion")
    public Flux<LlmWebMapper.StreamingChunkResponse> streamCompletion(
            @Valid @RequestBody CompletionRequest request,
            @RequestHeader("X-Organization-Id") String organizationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var command = mapper.toStreamCommand(request, organizationId, userDetails.getUsername());
        
        return streamCompletionUseCase.execute(command)
            .map(mapper::toStreamingResponse)
            .onErrorResume(throwable -> 
                Flux.just(new LlmWebMapper.StreamingChunkResponse(
                    "", false, true, "error", throwable.getMessage()
                ))
            );
    }
    
    @GetMapping("/providers")
    @Operation(summary = "List available LLM providers")
    public ResponseEntity<LlmWebMapper.ProvidersListResponse> listProviders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String capability,
            @RequestParam(required = false) String nameFilter,
            @RequestParam(defaultValue = "false") boolean includeMetrics,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var query = ListProvidersQuery.builder()
            .status(status)
            .capability(capability)
            .nameFilter(nameFilter)
            .includeMetrics(includeMetrics)
            .page(page)
            .size(size)
            .build();
        
        var result = listProvidersUseCase.execute(query);
        
        return ResponseEntity.ok(mapper.toResponse(result));
    }
    
    @PostMapping("/providers/{providerId}/health")
    @Operation(summary = "Check provider health status")
    public ResponseEntity<ProviderHealthResponse> checkProviderHealth(
            @PathVariable String providerId,
            @RequestParam(defaultValue = "false") boolean includeModels,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestHeader("X-Organization-Id") String organizationId
    ) {
        var command = new CheckProviderHealthCommand(
            providerId,
            organizationId,
            includeModels,
            forceRefresh
        );
        
        var result = checkProviderHealthUseCase.execute(command);
        
        return ResponseEntity.ok(new ProviderHealthResponse(
            result.providerId(),
            result.status(),
            result.message(),
            result.responseTimeMs(),
            result.checkedAt(),
            result.modelHealth(),
            result.metrics()
        ));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            IllegalArgumentException ex,
            @RequestAttribute("jakarta.servlet.error.request_uri") String path
    ) {
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.validation(ex.getMessage(), path));
    }
    
    @ExceptionHandler(com.zamaz.mcp.common.application.exception.ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            com.zamaz.mcp.common.application.exception.ResourceNotFoundException ex,
            @RequestAttribute("jakarta.servlet.error.request_uri") String path
    ) {
        return ResponseEntity
            .notFound()
            .build();
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleProviderError(
            RuntimeException ex,
            @RequestAttribute("jakarta.servlet.error.request_uri") String path
    ) {
        if (ex.getMessage() != null && ex.getMessage().contains("rate limit")) {
            return ResponseEntity
                .status(429)
                .body(ErrorResponse.rateLimited(ex.getMessage(), path));
        }
        
        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.providerError(ex.getMessage(), null, path));
    }
    
    /**
     * Response DTO for provider health checks.
     */
    public record ProviderHealthResponse(
        String providerId,
        String status,
        String message,
        long responseTimeMs,
        java.time.Instant checkedAt,
        java.util.List<com.zamaz.mcp.llm.application.query.ProviderHealthResult.ModelHealthInfo> modelHealth,
        com.zamaz.mcp.llm.application.query.ProviderHealthResult.HealthMetrics metrics
    ) {}
}