package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.llm.exception.LlmException;
import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompletionService {
    
    private final ProviderRegistry providerRegistry;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;
    
    @CircuitBreaker(name = "llm-completion", fallbackMethod = "completeFallback")
    @Retry(name = "llm-completion")
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        return rateLimitService.checkRateLimit(request.getProvider())
                .then(Mono.defer(() -> {
                    String cacheKey = cacheService.generateCacheKey(request);
                    
                    return cacheService.get(cacheKey, CompletionResponse.class)
                            .switchIfEmpty(Mono.defer(() -> {
                                var provider = providerRegistry.getProvider(request.getProvider())
                                        .orElseThrow(() -> new LlmException("Provider not found: " + request.getProvider()));
                                
                                if (!provider.isEnabled()) {
                                    throw new LlmException("Provider is disabled: " + request.getProvider());
                                }
                                
                                return provider.complete(request)
                                        .flatMap(response -> cacheService.put(cacheKey, response)
                                                .thenReturn(response));
                            }));
                }))
                .doOnSuccess(response -> log.info("Completion request successful for provider: {}", request.getProvider()))
                .doOnError(error -> log.error("Completion request failed for provider: {}", request.getProvider(), error));
    }
    
    public Flux<String> streamComplete(CompletionRequest request) {
        return rateLimitService.checkRateLimit(request.getProvider())
                .thenMany(Flux.defer(() -> {
                    var provider = providerRegistry.getProvider(request.getProvider())
                            .orElseThrow(() -> new LlmException("Provider not found: " + request.getProvider()));
                    
                    if (!provider.isEnabled()) {
                        throw new LlmException("Provider is disabled: " + request.getProvider());
                    }
                    
                    return provider.streamComplete(request);
                }))
                .doOnSubscribe(sub -> log.info("Stream completion started for provider: {}", request.getProvider()))
                .doOnComplete(() -> log.info("Stream completion completed for provider: {}", request.getProvider()))
                .doOnError(error -> log.error("Stream completion failed for provider: {}", request.getProvider(), error));
    }
    
    public Mono<CompletionResponse> completeFallback(CompletionRequest request, Exception ex) {
        log.error("Circuit breaker activated for provider: {}. Using fallback.", request.getProvider(), ex);
        
        return Mono.just(CompletionResponse.builder()
                .provider(request.getProvider())
                .choices(java.util.List.of(
                        CompletionResponse.Choice.builder()
                                .index(0)
                                .message(CompletionResponse.Message.builder()
                                        .role("assistant")
                                        .content("I'm sorry, but I'm currently experiencing technical difficulties. Please try again later.")
                                        .build())
                                .finishReason("error")
                                .build()
                ))
                .metadata(java.util.Map.of("error", ex.getMessage(), "fallback", true))
                .build());
    }
}