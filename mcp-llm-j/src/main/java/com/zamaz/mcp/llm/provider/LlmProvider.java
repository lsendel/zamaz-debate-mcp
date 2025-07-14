package com.zamaz.mcp.llm.provider;

import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LlmProvider {
    
    String getName();
    
    boolean isEnabled();
    
    List<String> getSupportedModels();
    
    String getDefaultModel();
    
    Mono<CompletionResponse> complete(CompletionRequest request);
    
    Flux<String> streamComplete(CompletionRequest request);
    
    Mono<Integer> countTokens(String text);
    
    Mono<Boolean> checkHealth();
}