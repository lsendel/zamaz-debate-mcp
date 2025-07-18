package com.zamaz.mcp.llm.adapter.external;

import com.zamaz.mcp.llm.application.port.outbound.LlmProviderGateway;
import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.domain.model.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import com.zamaz.mcp.common.resilience.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * External adapter implementation for Claude (Anthropic) API.
 * Implements LlmProviderGateway for Anthropic's Claude models.
 */
@Slf4j
@Component
@Qualifier("claudeProvider")
public class ClaudeProviderAdapter implements LlmProviderGateway {

    private static final ProviderId PROVIDER_ID = ProviderId.claude();
    private static final String API_VERSION = "2023-06-01";
    
    private final WebClient webClient;
    private final LlmProperties.ProviderConfig providerConfig;
    private final Timer completionTimer;
    private final Timer healthCheckTimer;

    public ClaudeProviderAdapter(
            @Qualifier("claudeWebClient") WebClient webClient,
            LlmProperties llmProperties,
            MeterRegistry meterRegistry
    ) {
        this.webClient = webClient;
        this.providerConfig = llmProperties.getProviders().get("claude");
        this.completionTimer = Timer.builder("llm.completion.duration")
                .tag("provider", "claude")
                .register(meterRegistry);
        this.healthCheckTimer = Timer.builder("llm.health_check.duration")
                .tag("provider", "claude")
                .register(meterRegistry);
        
        if (providerConfig == null) {
            throw new IllegalStateException("Claude provider configuration not found");
        }
    }

    @Override
    @CircuitBreaker(name = "claude-completion", fallbackMethod = "fallbackCompletion")
    @Retry(name = "claude-completion")
    public Mono<CompletionResponse> generateCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request
    ) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Mono.error(new IllegalArgumentException("Invalid provider ID: " + providerId));
        }

        log.debug("Generating completion for model {} with provider {}", model, providerId);
        
        var startTime = Instant.now();
        
        return createCompletionRequest(model, request)
                .flatMap(requestBody -> 
                    webClient.post()
                            .uri("/v1/messages")
                            .header("anthropic-version", API_VERSION)
                            .header("x-api-key", providerConfig.getApiKey())
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(ClaudeResponse.class)
                )
                .map(response -> mapToCompletionResponse(response, startTime))
                .doOnSuccess(response -> {
                    completionTimer.record(Duration.between(startTime, Instant.now()));
                    log.debug("Completion generated successfully for model {}", model);
                })
                .doOnError(error -> {
                    log.error("Error generating completion for model {}: {}", model, error.getMessage());
                    completionTimer.record(Duration.between(startTime, Instant.now()));
                });
    }

    @Override
    @CircuitBreaker(name = "claude-streaming", fallbackMethod = "fallbackStreamingCompletion")
    @Retry(name = "claude-streaming")
    public Flux<CompletionChunk> generateStreamingCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request
    ) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Flux.error(new IllegalArgumentException("Invalid provider ID: " + providerId));
        }

        log.debug("Generating streaming completion for model {} with provider {}", model, providerId);
        
        return createStreamingCompletionRequest(model, request)
                .flatMapMany(requestBody ->
                    webClient.post()
                            .uri("/v1/messages")
                            .header("anthropic-version", API_VERSION)
                            .header("x-api-key", providerConfig.getApiKey())
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToFlux(String.class)
                )
                .map(this::parseStreamingChunk)
                .filter(chunk -> chunk != null)
                .doOnComplete(() -> log.debug("Streaming completion finished for model {}", model))
                .doOnError(error -> log.error("Error in streaming completion for model {}: {}", model, error.getMessage()));
    }

    @Override
    @CircuitBreaker(name = "claude-health", fallbackMethod = "fallbackHealthCheck")
    public Mono<ProviderHealthCheck> checkProviderHealth(ProviderId providerId) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Mono.just(new ProviderHealthCheck(
                    providerId,
                    ProviderStatus.UNAVAILABLE,
                    "Invalid provider ID",
                    0L,
                    Instant.now()
            ));
        }

        var startTime = Instant.now();
        
        return webClient.get()
                .uri("/v1/messages")
                .header("anthropic-version", API_VERSION)
                .header("x-api-key", providerConfig.getApiKey())
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    var responseTime = Duration.between(startTime, Instant.now()).toMillis();
                    healthCheckTimer.record(Duration.ofMillis(responseTime));
                    
                    var status = response.getStatusCode().is2xxSuccessful() ? 
                            ProviderStatus.AVAILABLE : ProviderStatus.DEGRADED;
                    
                    return new ProviderHealthCheck(
                            PROVIDER_ID,
                            status,
                            "Provider is responsive",
                            responseTime,
                            Instant.now()
                    );
                })
                .onErrorReturn(new ProviderHealthCheck(
                        PROVIDER_ID,
                        ProviderStatus.UNAVAILABLE,
                        "Provider health check failed",
                        Duration.between(startTime, Instant.now()).toMillis(),
                        Instant.now()
                ));
    }

    @Override
    public Mono<List<ModelInfo>> getAvailableModels(ProviderId providerId) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Mono.just(List.of());
        }

        // Claude models are statically defined as they don't have a models endpoint
        var models = List.of(
            new ModelInfo(
                ModelName.claude3Opus(),
                "Claude 3 Opus",
                200000,
                true,
                true,
                false
            ),
            new ModelInfo(
                ModelName.claude3Sonnet(),
                "Claude 3 Sonnet",
                200000,
                true,
                true,
                false
            ),
            new ModelInfo(
                ModelName.claude3Haiku(),
                "Claude 3 Haiku",
                200000,
                true,
                true,
                false
            )
        );

        return Mono.just(models);
    }

    @Override
    public Mono<Integer> estimateTokenCount(ProviderId providerId, ModelName model, PromptContent prompt) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Mono.just(0);
        }

        // Claude uses approximately 1 token per 3.5 characters for English text
        int estimatedTokens = Math.max(1, prompt.value().length() / 4);
        return Mono.just(estimatedTokens);
    }

    @Override
    public boolean supportsCapability(ProviderId providerId, LlmModel.ModelCapability capability) {
        if (!PROVIDER_ID.equals(providerId)) {
            return false;
        }

        return switch (capability) {
            case TEXT_COMPLETION -> true;
            case STREAMING -> true;
            case SYSTEM_MESSAGES -> true;
            case VISION -> false; // Vision support in Claude 3 varies by model
        };
    }

    private Mono<Map<String, Object>> createCompletionRequest(ModelName model, CompletionRequest request) {
        return Mono.fromCallable(() -> Map.of(
                "model", model.value(),
                "max_tokens", request.getMaxTokens(),
                "temperature", request.getTemperature(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", request.getPrompt().value()
                )),
                "stream", false
        ));
    }

    private Mono<Map<String, Object>> createStreamingCompletionRequest(ModelName model, CompletionRequest request) {
        return Mono.fromCallable(() -> Map.of(
                "model", model.value(),
                "max_tokens", request.getMaxTokens(),
                "temperature", request.getTemperature(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", request.getPrompt().value()
                )),
                "stream", true
        ));
    }

    private CompletionResponse mapToCompletionResponse(ClaudeResponse response, Instant startTime) {
        var latency = Duration.between(startTime, Instant.now()).toMillis();
        
        var usage = TokenUsage.of(
                response.usage().inputTokens(),
                response.usage().outputTokens()
        );

        var content = response.content().isEmpty() ? "" : 
                response.content().get(0).text();

        return new CompletionResponse(
                content,
                usage,
                response.stopReason(),
                response.model(),
                latency
        );
    }

    private CompletionChunk parseStreamingChunk(String chunk) {
        // Parse Server-Sent Events format from Claude
        if (chunk.startsWith("data: ")) {
            String jsonData = chunk.substring(6);
            if ("[DONE]".equals(jsonData.trim())) {
                return new CompletionChunk("", true, "stop");
            }
            
            try {
                // Parse JSON and extract content
                // This is simplified - in real implementation, use a JSON library
                if (jsonData.contains("\"content\"")) {
                    String content = extractContent(jsonData);
                    boolean isComplete = jsonData.contains("\"stop_reason\"");
                    String finishReason = isComplete ? "stop" : null;
                    
                    return new CompletionChunk(content, isComplete, finishReason);
                }
            } catch (Exception e) {
                log.warn("Failed to parse streaming chunk: {}", e.getMessage());
            }
        }
        
        return null;
    }

    private String extractContent(String jsonData) {
        // Simple content extraction - in production use proper JSON parsing
        int startIndex = jsonData.indexOf("\"text\":\"") + 8;
        if (startIndex < 8) return "";
        
        int endIndex = jsonData.indexOf("\"", startIndex);
        if (endIndex == -1) return "";
        
        return jsonData.substring(startIndex, endIndex);
    }

    // Fallback methods for circuit breaker
    private Mono<CompletionResponse> fallbackCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request,
            Exception ex
    ) {
        log.error("Claude completion circuit breaker activated", ex);
        return Mono.error(new RuntimeException("Claude provider temporarily unavailable", ex));
    }

    private Flux<CompletionChunk> fallbackStreamingCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request,
            Exception ex
    ) {
        log.error("Claude streaming circuit breaker activated", ex);
        return Flux.error(new RuntimeException("Claude streaming temporarily unavailable", ex));
    }

    private Mono<ProviderHealthCheck> fallbackHealthCheck(ProviderId providerId, Exception ex) {
        log.error("Claude health check circuit breaker activated", ex);
        return Mono.just(new ProviderHealthCheck(
                PROVIDER_ID,
                ProviderStatus.UNAVAILABLE,
                "Provider circuit breaker activated: " + ex.getMessage(),
                0L,
                Instant.now()
        ));
    }

    // Claude API response DTOs
    private record ClaudeResponse(
            String id,
            String type,
            String role,
            List<ClaudeContent> content,
            String model,
            String stopReason,
            String stopSequence,
            ClaudeUsage usage
    ) {}

    private record ClaudeContent(
            String type,
            String text
    ) {}

    private record ClaudeUsage(
            int inputTokens,
            int outputTokens
    ) {}
}