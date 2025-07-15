package com.zamaz.mcp.llm.provider;

import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.exception.LlmException;
import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

/**
 * Claude AI provider implementation for LLM services.
 * Supports Claude 3 and Claude 2 models from Anthropic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeProvider implements LlmProvider {
    
    private final WebClient.Builder webClientBuilder;
    private final LlmProperties llmProperties;
    
    private static final String PROVIDER_NAME = "claude";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String API_PATH = "/v1/messages";
    private static final int TOKENS_PER_CHAR_ESTIMATE = 4;
    
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307",
        "claude-2.1",
        "claude-instant-1.2"
    );
    
    // Pricing per 1M tokens (as of 2024)
    private static final Map<String, ModelPricing> MODEL_PRICING = Map.of(
        "claude-3-opus-20240229", new ModelPricing(15.0, 75.0),
        "claude-3-sonnet-20240229", new ModelPricing(3.0, 15.0),
        "claude-3-haiku-20240307", new ModelPricing(0.25, 1.25),
        "claude-2.1", new ModelPricing(8.0, 24.0),
        "claude-instant-1.2", new ModelPricing(0.8, 2.4)
    );
    
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public boolean isEnabled() {
        var config = getProviderConfig();
        return config != null && config.isEnabled();
    }
    
    @Override
    public List<String> getSupportedModels() {
        return SUPPORTED_MODELS;
    }
    
    @Override
    public String getDefaultModel() {
        return getProviderConfig().getDefaultModel();
    }
    
    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        var config = getProviderConfig();
        var startTime = System.currentTimeMillis();
        
        return Mono.defer(() -> {
            try {
                String model = resolveModel(request, config);
                Map<String, Object> requestBody = buildRequestBody(request, config, model, false);
                
                return executeApiCall(config, requestBody)
                    .map(response -> mapToCompletionResponse(response, model, startTime));
            } catch (Exception e) {
                return Mono.error(new LlmException("Failed to process completion request", e));
            }
        });
    }
    
    @Override
    public Flux<String> streamComplete(CompletionRequest request) {
        var config = getProviderConfig();
        
        return Flux.defer(() -> {
            try {
                String model = resolveModel(request, config);
                Map<String, Object> requestBody = buildRequestBody(request, config, model, true);
                
                return executeStreamingApiCall(config, requestBody);
            } catch (Exception e) {
                return Flux.error(new LlmException("Failed to process streaming request", e));
            }
        });
    }
    
    @Override
    public Mono<Integer> countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return Mono.just(0);
        }
        // Claude doesn't provide a token counting API, so we estimate
        // This is a rough approximation and should be improved with a proper tokenizer
        return Mono.just(text.length() / TOKENS_PER_CHAR_ESTIMATE);
    }
    
    @Override
    public Mono<Boolean> checkHealth() {
        var config = getProviderConfig();
        if (config == null || !config.isEnabled()) {
            return Mono.just(false);
        }
        
        return webClientBuilder.build()
            .get()
            .uri(config.getBaseUrl() + API_PATH)
            .headers(headers -> addAuthHeaders(headers, config))
            .retrieve()
            .toBodilessEntity()
            .map(response -> response.getStatusCode().is2xxSuccessful())
            .doOnError(error -> log.warn("Health check failed for Claude provider", error))
            .onErrorReturn(false);
    }
    
    /**
     * Get provider configuration
     */
    private LlmProperties.ProviderConfig getProviderConfig() {
        var providers = llmProperties.getProviders();
        if (providers == null || !providers.containsKey(PROVIDER_NAME)) {
            throw new LlmException("Claude provider configuration not found");
        }
        return providers.get(PROVIDER_NAME);
    }
    
    /**
     * Resolve the model to use
     */
    private String resolveModel(CompletionRequest request, LlmProperties.ProviderConfig config) {
        String model = request.getModel() != null ? request.getModel() : config.getDefaultModel();
        if (!SUPPORTED_MODELS.contains(model)) {
            throw new LlmException("Unsupported model: " + model);
        }
        return model;
    }
    
    /**
     * Build the request body for Claude API
     */
    private Map<String, Object> buildRequestBody(CompletionRequest request, 
                                                 LlmProperties.ProviderConfig config,
                                                 String model, 
                                                 boolean streaming) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", convertMessages(request.getMessages()));
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens());
        
        if (streaming) {
            body.put("stream", true);
        }
        
        addOptionalParameters(body, request);
        return body;
    }
    
    /**
     * Add optional parameters to request body
     */
    private void addOptionalParameters(Map<String, Object> body, CompletionRequest request) {
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        
        if (request.getSystemPrompt() != null) {
            body.put("system", request.getSystemPrompt());
        }
        
        if (request.getStopSequences() != null && !request.getStopSequences().isEmpty()) {
            body.put("stop_sequences", request.getStopSequences());
        }
    }
    
    /**
     * Execute API call to Claude
     */
    private Mono<Map> executeApiCall(LlmProperties.ProviderConfig config, Map<String, Object> requestBody) {
        return webClientBuilder.build()
            .post()
            .uri(config.getBaseUrl() + API_PATH)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                addAuthHeaders(headers, config);
            })
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .doOnError(error -> log.error("Error calling Claude API", error));
    }
    
    /**
     * Execute streaming API call to Claude
     */
    private Flux<String> executeStreamingApiCall(LlmProperties.ProviderConfig config, Map<String, Object> requestBody) {
        return webClientBuilder.build()
            .post()
            .uri(config.getBaseUrl() + API_PATH)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                addAuthHeaders(headers, config);
            })
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: "))
            .map(line -> line.substring(6))
            .filter(data -> !data.equals("[DONE]"))
            .doOnError(error -> log.error("Error streaming from Claude API", error));
    }
    
    /**
     * Add authentication headers
     */
    private void addAuthHeaders(HttpHeaders headers, LlmProperties.ProviderConfig config) {
        headers.set("x-api-key", config.getApiKey());
        headers.set("anthropic-version", ANTHROPIC_VERSION);
    }
    
    /**
     * Convert messages to Claude format
     */
    private List<Map<String, String>> convertMessages(List<CompletionRequest.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        
        return messages.stream()
            .filter(msg -> msg != null && msg.getRole() != null && msg.getContent() != null)
            .filter(msg -> !msg.getRole().equals("system")) // Claude handles system messages differently
            .map(msg -> Map.of(
                "role", msg.getRole(),
                "content", msg.getContent()
            ))
            .toList();
    }
    
    /**
     * Map Claude API response to CompletionResponse
     */
    private CompletionResponse mapToCompletionResponse(Map<String, Object> response, String model, long startTime) {
        if (response == null) {
            throw new LlmException("Empty response from Claude API");
        }
        
        String content = extractContent(response);
        CompletionResponse.Usage usage = extractUsage(response, model);
        String finishReason = (String) response.get("stop_reason");
        
        CompletionResponse.Message message = CompletionResponse.Message.builder()
            .role("assistant")
            .content(content)
            .build();
        
        CompletionResponse.Choice choice = CompletionResponse.Choice.builder()
            .index(0)
            .message(message)
            .finishReason(finishReason)
            .build();
        
        return CompletionResponse.builder()
            .id((String) response.get("id"))
            .provider(PROVIDER_NAME)
            .model(model)
            .choices(Collections.singletonList(choice))
            .usage(usage)
            .createdAt(Instant.now())
            .processingTimeMs(System.currentTimeMillis() - startTime)
            .build();
    }
    
    /**
     * Extract content from response
     */
    private String extractContent(Map<String, Object> response) {
        Object content = response.get("content");
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof List) {
            // Handle structured content responses
            List<?> contentList = (List<?>) content;
            if (!contentList.isEmpty() && contentList.get(0) instanceof Map) {
                Map<?, ?> firstContent = (Map<?, ?>) contentList.get(0);
                Object text = firstContent.get("text");
                if (text instanceof String) {
                    return (String) text;
                }
            }
        }
        throw new LlmException("Unable to extract content from Claude response");
    }
    
    /**
     * Extract usage information from response
     */
    private CompletionResponse.Usage extractUsage(Map<String, Object> response, String model) {
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage == null) {
            return null;
        }
        
        Integer inputTokens = extractInteger(usage, "input_tokens");
        Integer outputTokens = extractInteger(usage, "output_tokens");
        
        if (inputTokens == null || outputTokens == null) {
            log.warn("Incomplete usage information in Claude response");
            return null;
        }
        
        return CompletionResponse.Usage.builder()
            .promptTokens(inputTokens)
            .completionTokens(outputTokens)
            .totalTokens(inputTokens + outputTokens)
            .estimatedCost(calculateCost(model, inputTokens, outputTokens))
            .build();
    }
    
    /**
     * Safely extract integer from map
     */
    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    /**
     * Calculate cost based on model pricing
     */
    private double calculateCost(String model, int inputTokens, int outputTokens) {
        ModelPricing pricing = MODEL_PRICING.getOrDefault(model, MODEL_PRICING.get("claude-2.1"));
        
        double inputCost = (inputTokens * pricing.inputCostPerMillion) / 1_000_000.0;
        double outputCost = (outputTokens * pricing.outputCostPerMillion) / 1_000_000.0;
        
        return inputCost + outputCost;
    }
    
    /**
     * Model pricing information
     */
    private record ModelPricing(double inputCostPerMillion, double outputCostPerMillion) {}
}