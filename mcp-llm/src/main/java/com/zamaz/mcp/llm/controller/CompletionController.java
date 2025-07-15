package com.zamaz.mcp.llm.controller;

import com.zamaz.mcp.common.dto.ApiResponse;
import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import com.zamaz.mcp.llm.service.CompletionService;
import com.zamaz.mcp.llm.service.ProviderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for LLM completion operations using Spring AI.
 * Provides endpoints for text generation, streaming, and provider management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/completions")
@RequiredArgsConstructor
@Tag(name = "Completions", description = "LLM completion endpoints using Spring AI")
public class CompletionController {
    
    private final CompletionService completionService;
    private final ProviderRegistry providerRegistry;
    
    /**
     * Generate a completion using the specified LLM provider
     */
    @PostMapping
    @Operation(summary = "Generate completion", 
               description = "Generate a text completion using the specified LLM provider")
    public Mono<ApiResponse<CompletionResponse>> complete(
            @Valid @RequestBody CompletionRequest request) {
        log.info("Completion request for provider: {}, model: {}", 
                request.getProvider(), request.getModel());
        
        return completionService.complete(request)
            .map(ApiResponse::success)
            .doOnSuccess(response -> log.debug("Completion successful for provider: {}", 
                    request.getProvider()))
            .doOnError(error -> log.error("Completion failed for provider {}: {}", 
                    request.getProvider(), error.getMessage()));
    }
    
    /**
     * Generate a streaming completion
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Generate streaming completion", 
               description = "Generate a streaming text completion using the specified LLM provider")
    public Flux<String> streamComplete(
            @Valid @RequestBody CompletionRequest request) {
        log.info("Streaming completion request for provider: {}, model: {}", 
                request.getProvider(), request.getModel());
        
        return completionService.streamComplete(request)
            .doOnSubscribe(subscription -> log.debug("Started streaming for provider: {}", 
                    request.getProvider()))
            .doOnComplete(() -> log.debug("Completed streaming for provider: {}", 
                    request.getProvider()))
            .doOnError(error -> log.error("Streaming failed for provider {}: {}", 
                    request.getProvider(), error.getMessage()));
    }
    
    /**
     * Get list of available LLM providers
     */
    @GetMapping("/providers")
    @Operation(summary = "Get available providers", 
               description = "Get list of currently available LLM providers")
    public Mono<ApiResponse<List<String>>> getProviders() {
        log.debug("Fetching available providers");
        
        return Mono.fromCallable(() -> 
            providerRegistry.getEnabledProviders().stream()
                .map(provider -> provider.getName())
                .collect(Collectors.toList())
        ).map(ApiResponse::success);
    }
    
    /**
     * Check health of a specific provider
     */
    @GetMapping("/providers/{provider}/health")
    @Operation(summary = "Check provider health", 
               description = "Check if the specified LLM provider is available and healthy")
    public Mono<ApiResponse<Boolean>> checkProviderHealth(
            @Parameter(description = "LLM provider name") 
            @PathVariable String provider) {
        log.debug("Checking health for provider: {}", provider);
        
        return Mono.fromCallable(() -> 
            providerRegistry.getProvider(provider)
                .map(p -> p.isEnabled())
                .orElse(false)
        ).map(ApiResponse::success);
    }
    
    /**
     * Health check endpoint for the completion service
     */
    @GetMapping("/health")
    @Operation(summary = "Service health check", 
               description = "Check if the completion service is healthy")
    public Mono<ApiResponse<String>> health() {
        return Mono.just(ApiResponse.success("Completion service is healthy"));
    }
}