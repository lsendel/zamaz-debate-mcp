package com.zamaz.mcp.llm.controller;

import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import com.zamaz.mcp.llm.service.CompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/completions")
@RequiredArgsConstructor
@Tag(name = "Completions", description = "LLM completion endpoints")
public class CompletionController {
    
    private final CompletionService completionService;
    
    @PostMapping
    @Operation(summary = "Generate completion")
    public Mono<CompletionResponse> complete(@Valid @RequestBody CompletionRequest request) {
        return completionService.complete(request);
    }
    
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Generate streaming completion")
    public Flux<String> streamComplete(@Valid @RequestBody CompletionRequest request) {
        return completionService.streamComplete(request);
    }
}