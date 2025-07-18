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
 * External adapter implementation for OpenAI API.
 * Implements LlmProviderGateway for OpenAI's GPT models.
 */
@Slf4j
@Component
@Qualifier("openaiProvider")
public class OpenAiProviderAdapter implements LlmProviderGateway {

    private static final ProviderId PROVIDER_ID = ProviderId.openai();
    
    private final WebClient webClient;
    private final LlmProperties.ProviderConfig providerConfig;
    private final Timer completionTimer;
    private final Timer healthCheckTimer;

    public OpenAiProviderAdapter(
            @Qualifier("openaiWebClient") WebClient webClient,
            LlmProperties llmProperties,
            MeterRegistry meterRegistry
    ) {
        this.webClient = webClient;
        this.providerConfig = llmProperties.getProviders().get("openai");
        this.completionTimer = Timer.builder("llm.completion.duration")
                .tag("provider", "openai")
                .register(meterRegistry);
        this.healthCheckTimer = Timer.builder("llm.health_check.duration")
                .tag("provider", "openai")
                .register(meterRegistry);
        
        if (providerConfig == null) {
            throw new IllegalStateException("OpenAI provider configuration not found");
        }
    }

    @Override
    @CircuitBreaker(name = "openai-completion", fallbackMethod = "fallbackCompletion")
    @Retry(name = "openai-completion")
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
                            .uri("/v1/chat/completions")
                            .header("Authorization", "Bearer " + providerConfig.getApiKey())
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(OpenAiResponse.class)
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
    @CircuitBreaker(name = "openai-streaming", fallbackMethod = "fallbackStreamingCompletion")
    @Retry(name = "openai-streaming")
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
                            .uri("/v1/chat/completions")
                            .header("Authorization", "Bearer " + providerConfig.getApiKey())
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
    @CircuitBreaker(name = "openai-health", fallbackMethod = "fallbackHealthCheck")
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
                .uri("/v1/models")
                .header("Authorization", "Bearer " + providerConfig.getApiKey())
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

        return webClient.get()
                .uri("/v1/models")
                .header("Authorization", "Bearer " + providerConfig.getApiKey())
                .retrieve()
                .bodyToMono(OpenAiModelsResponse.class)
                .map(response -> response.data().stream()
                        .filter(model -> model.id().startsWith("gpt-"))
                        .map(this::mapToModelInfo)
                        .toList())
                .onErrorReturn(getStaticModels());
    }

    @Override
    public Mono<Integer> estimateTokenCount(ProviderId providerId, ModelName model, PromptContent prompt) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Mono.just(0);
        }

        // OpenAI uses approximately 1 token per 4 characters for English text
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
            case VISION -> true; // GPT-4 Vision models support this
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

    private CompletionResponse mapToCompletionResponse(OpenAiResponse response, Instant startTime) {
        var latency = Duration.between(startTime, Instant.now()).toMillis();
        
        var usage = TokenUsage.of(
                response.usage().promptTokens(),
                response.usage().completionTokens()
        );

        var content = response.choices().isEmpty() ? "" : 
                response.choices().get(0).message().content();

        var finishReason = response.choices().isEmpty() ? "unknown" :
                response.choices().get(0).finishReason();

        return new CompletionResponse(
                content,
                usage,
                finishReason,
                response.model(),
                latency
        );
    }

    private CompletionChunk parseStreamingChunk(String chunk) {
        // Parse Server-Sent Events format from OpenAI
        if (chunk.startsWith("data: ")) {
            String jsonData = chunk.substring(6);
            if ("[DONE]".equals(jsonData.trim())) {
                return new CompletionChunk("", true, "stop");
            }
            
            try {
                // Parse JSON and extract content
                if (jsonData.contains("\"delta\"")) {
                    String content = extractDeltaContent(jsonData);
                    boolean isComplete = jsonData.contains("\"finish_reason\"");
                    String finishReason = isComplete ? extractFinishReason(jsonData) : null;
                    
                    return new CompletionChunk(content, isComplete, finishReason);
                }
            } catch (Exception e) {
                log.warn("Failed to parse streaming chunk: {}", e.getMessage());
            }
        }
        
        return null;
    }

    private String extractDeltaContent(String jsonData) {
        // Simple content extraction from delta - in production use proper JSON parsing
        int startIndex = jsonData.indexOf("\"content\":\"") + 11;
        if (startIndex < 11) return "";
        
        int endIndex = jsonData.indexOf("\"", startIndex);
        if (endIndex == -1) return "";
        
        return jsonData.substring(startIndex, endIndex);
    }

    private String extractFinishReason(String jsonData) {
        int startIndex = jsonData.indexOf("\"finish_reason\":\"") + 17;
        if (startIndex < 17) return "stop";
        
        int endIndex = jsonData.indexOf("\"", startIndex);
        if (endIndex == -1) return "stop";
        
        return jsonData.substring(startIndex, endIndex);
    }

    private ModelInfo mapToModelInfo(OpenAiModel model) {
        return new ModelInfo(
                ModelName.of(model.id()),
                model.id(),
                getMaxTokensForModel(model.id()),
                true, // Most OpenAI models support streaming
                true, // Support system messages
                model.id().contains("vision") // Vision support
        );
    }

    private int getMaxTokensForModel(String modelId) {
        return switch (modelId) {
            case "gpt-4-turbo-preview", "gpt-4-vision-preview" -> 128000;
            case "gpt-4", "gpt-4-32k" -> 32768;
            case "gpt-3.5-turbo" -> 16385;
            default -> 4096;
        };
    }

    private List<ModelInfo> getStaticModels() {
        return List.of(
            new ModelInfo(
                ModelName.gpt4Turbo(),
                "GPT-4 Turbo",
                128000,
                true,
                true,
                true
            ),
            new ModelInfo(
                ModelName.gpt4(),
                "GPT-4",
                8192,
                true,
                true,
                false
            ),
            new ModelInfo(
                ModelName.gpt35Turbo(),
                "GPT-3.5 Turbo",
                16385,
                true,
                true,
                false
            )
        );
    }

    // Fallback methods for circuit breaker
    private Mono<CompletionResponse> fallbackCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request,
            Exception ex
    ) {
        log.error("OpenAI completion circuit breaker activated", ex);
        return Mono.error(new RuntimeException("OpenAI provider temporarily unavailable", ex));
    }

    private Flux<CompletionChunk> fallbackStreamingCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request,
            Exception ex
    ) {
        log.error("OpenAI streaming circuit breaker activated", ex);
        return Flux.error(new RuntimeException("OpenAI streaming temporarily unavailable", ex));
    }

    private Mono<ProviderHealthCheck> fallbackHealthCheck(ProviderId providerId, Exception ex) {
        log.error("OpenAI health check circuit breaker activated", ex);
        return Mono.just(new ProviderHealthCheck(
                PROVIDER_ID,
                ProviderStatus.UNAVAILABLE,
                "Provider circuit breaker activated: " + ex.getMessage(),
                0L,
                Instant.now()
        ));
    }

    // OpenAI API response DTOs
    private record OpenAiResponse(
            String id,
            String object,
            long created,
            String model,
            List<OpenAiChoice> choices,
            OpenAiUsage usage
    ) {}

    private record OpenAiChoice(
            int index,
            OpenAiMessage message,
            String finishReason
    ) {}

    private record OpenAiMessage(
            String role,
            String content
    ) {}

    private record OpenAiUsage(
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {}

    private record OpenAiModelsResponse(
            String object,
            List<OpenAiModel> data
    ) {}

    private record OpenAiModel(
            String id,
            String object,
            long created,
            String ownedBy
    ) {}
}