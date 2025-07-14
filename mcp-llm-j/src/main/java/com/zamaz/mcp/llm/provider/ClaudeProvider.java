package com.zamaz.mcp.llm.provider;

import com.zamaz.mcp.llm.config.LlmProperties;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeProvider implements LlmProvider {
    
    private final WebClient.Builder webClientBuilder;
    private final LlmProperties llmProperties;
    
    private static final String PROVIDER_NAME = "claude";
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307",
        "claude-2.1",
        "claude-instant-1.2"
    );
    
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public boolean isEnabled() {
        return llmProperties.getProviders().get(PROVIDER_NAME).isEnabled();
    }
    
    @Override
    public List<String> getSupportedModels() {
        return SUPPORTED_MODELS;
    }
    
    @Override
    public String getDefaultModel() {
        return llmProperties.getProviders().get(PROVIDER_NAME).getDefaultModel();
    }
    
    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        var config = llmProperties.getProviders().get(PROVIDER_NAME);
        var startTime = System.currentTimeMillis();
        
        String model = request.getModel() != null ? request.getModel() : config.getDefaultModel();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", convertMessages(request.getMessages()));
        requestBody.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens());
        
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        
        if (request.getSystemPrompt() != null) {
            requestBody.put("system", request.getSystemPrompt());
        }
        
        if (request.getStopSequences() != null && !request.getStopSequences().isEmpty()) {
            requestBody.put("stop_sequences", request.getStopSequences());
        }
        
        return webClientBuilder.build()
                .post()
                .uri(config.getBaseUrl() + "/v1/messages")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> mapToCompletionResponse(response, model, startTime))
                .doOnError(error -> log.error("Error calling Claude API: ", error));
    }
    
    @Override
    public Flux<String> streamComplete(CompletionRequest request) {
        var config = llmProperties.getProviders().get(PROVIDER_NAME);
        
        String model = request.getModel() != null ? request.getModel() : config.getDefaultModel();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", convertMessages(request.getMessages()));
        requestBody.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens());
        requestBody.put("stream", true);
        
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        
        if (request.getSystemPrompt() != null) {
            requestBody.put("system", request.getSystemPrompt());
        }
        
        return webClientBuilder.build()
                .post()
                .uri(config.getBaseUrl() + "/v1/messages")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(data -> !data.equals("[DONE]"))
                .doOnError(error -> log.error("Error streaming from Claude API: ", error));
    }
    
    @Override
    public Mono<Integer> countTokens(String text) {
        // Claude doesn't provide a token counting API, so we'll estimate
        // Rough estimation: 1 token ≈ 4 characters
        return Mono.just(text.length() / 4);
    }
    
    @Override
    public Mono<Boolean> checkHealth() {
        var config = llmProperties.getProviders().get(PROVIDER_NAME);
        
        return webClientBuilder.build()
                .get()
                .uri(config.getBaseUrl() + "/v1/messages")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
    
    private List<Map<String, String>> convertMessages(List<CompletionRequest.Message> messages) {
        return messages.stream()
                .filter(msg -> !msg.getRole().equals("system")) // Claude handles system messages differently
                .map(msg -> {
                    Map<String, String> message = new HashMap<>();
                    message.put("role", msg.getRole());
                    message.put("content", msg.getContent());
                    return message;
                })
                .toList();
    }
    
    private CompletionResponse mapToCompletionResponse(Map<String, Object> response, String model, long startTime) {
        String content = (String) response.get("content");
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        
        CompletionResponse.Message message = CompletionResponse.Message.builder()
                .role("assistant")
                .content(content)
                .build();
        
        CompletionResponse.Choice choice = CompletionResponse.Choice.builder()
                .index(0)
                .message(message)
                .finishReason((String) response.get("stop_reason"))
                .build();
        
        CompletionResponse.Usage usageInfo = null;
        if (usage != null) {
            usageInfo = CompletionResponse.Usage.builder()
                    .promptTokens((Integer) usage.get("input_tokens"))
                    .completionTokens((Integer) usage.get("output_tokens"))
                    .totalTokens((Integer) usage.get("input_tokens") + (Integer) usage.get("output_tokens"))
                    .estimatedCost(calculateCost(model, (Integer) usage.get("input_tokens"), (Integer) usage.get("output_tokens")))
                    .build();
        }
        
        return CompletionResponse.builder()
                .id((String) response.get("id"))
                .provider(PROVIDER_NAME)
                .model(model)
                .choices(Collections.singletonList(choice))
                .usage(usageInfo)
                .createdAt(Instant.now())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
    
    private double calculateCost(String model, int inputTokens, int outputTokens) {
        // Claude pricing per 1M tokens (as of 2024)
        double inputCostPerMillion = 0;
        double outputCostPerMillion = 0;
        
        switch (model) {
            case "claude-3-opus-20240229":
                inputCostPerMillion = 15.0;
                outputCostPerMillion = 75.0;
                break;
            case "claude-3-sonnet-20240229":
                inputCostPerMillion = 3.0;
                outputCostPerMillion = 15.0;
                break;
            case "claude-3-haiku-20240307":
                inputCostPerMillion = 0.25;
                outputCostPerMillion = 1.25;
                break;
            default:
                inputCostPerMillion = 8.0;
                outputCostPerMillion = 24.0;
        }
        
        return (inputTokens * inputCostPerMillion / 1_000_000.0) + 
               (outputTokens * outputCostPerMillion / 1_000_000.0);
    }
}