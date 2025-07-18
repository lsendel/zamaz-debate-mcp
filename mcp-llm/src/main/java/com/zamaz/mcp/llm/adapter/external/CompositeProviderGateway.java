package com.zamaz.mcp.llm.adapter.external;

import com.zamaz.mcp.llm.application.port.outbound.LlmProviderGateway;
import com.zamaz.mcp.llm.domain.model.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composite implementation of LlmProviderGateway that delegates to specific provider adapters
 * based on the ProviderId. Acts as a dispatcher/factory for different LLM providers.
 */
@Slf4j
@Component
@Qualifier("compositeProviderGateway")
public class CompositeProviderGateway implements LlmProviderGateway {

    private final Map<ProviderId, LlmProviderGateway> providerGateways;
    private final Counter requestCounter;
    private final Counter errorCounter;

    public CompositeProviderGateway(
            @Qualifier("claudeProvider") ClaudeProviderAdapter claudeProvider,
            @Qualifier("openaiProvider") OpenAiProviderAdapter openaiProvider,
            MeterRegistry meterRegistry
    ) {
        this.providerGateways = new ConcurrentHashMap<>();
        
        // Register available providers
        this.providerGateways.put(ProviderId.claude(), claudeProvider);
        this.providerGateways.put(ProviderId.openai(), openaiProvider);
        
        // Initialize metrics
        this.requestCounter = Counter.builder("llm.provider.requests")
                .description("Number of requests per provider")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("llm.provider.errors")
                .description("Number of errors per provider")
                .register(meterRegistry);
        
        log.info("CompositeProviderGateway initialized with {} providers: {}", 
                providerGateways.size(), providerGateways.keySet());
    }

    @Override
    public Mono<CompletionResponse> generateCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request
    ) {
        log.debug("Routing completion request to provider: {} for model: {}", providerId, model);
        
        return getProviderGateway(providerId)
                .flatMap(gateway -> {
                    requestCounter.increment("provider", providerId.value(), "operation", "completion");
                    
                    return gateway.generateCompletion(providerId, model, request)
                            .doOnSuccess(response -> {
                                log.debug("Completion request successful for provider: {} model: {}", 
                                        providerId, model);
                            })
                            .doOnError(error -> {
                                errorCounter.increment("provider", providerId.value(), "operation", "completion");
                                log.error("Completion request failed for provider: {} model: {}: {}", 
                                        providerId, model, error.getMessage());
                            });
                });
    }

    @Override
    public Flux<CompletionChunk> generateStreamingCompletion(
            ProviderId providerId,
            ModelName model,
            CompletionRequest request
    ) {
        log.debug("Routing streaming completion request to provider: {} for model: {}", providerId, model);
        
        return getProviderGateway(providerId)
                .flatMapMany(gateway -> {
                    requestCounter.increment("provider", providerId.value(), "operation", "streaming");
                    
                    return gateway.generateStreamingCompletion(providerId, model, request)
                            .doOnComplete(() -> {
                                log.debug("Streaming completion request completed for provider: {} model: {}", 
                                        providerId, model);
                            })
                            .doOnError(error -> {
                                errorCounter.increment("provider", providerId.value(), "operation", "streaming");
                                log.error("Streaming completion request failed for provider: {} model: {}: {}", 
                                        providerId, model, error.getMessage());
                            });
                });
    }

    @Override
    public Mono<ProviderHealthCheck> checkProviderHealth(ProviderId providerId) {
        log.debug("Checking health for provider: {}", providerId);
        
        return getProviderGateway(providerId)
                .flatMap(gateway -> {
                    requestCounter.increment("provider", providerId.value(), "operation", "health_check");
                    
                    return gateway.checkProviderHealth(providerId)
                            .doOnSuccess(health -> {
                                log.debug("Health check completed for provider: {} status: {}", 
                                        providerId, health.status());
                            })
                            .doOnError(error -> {
                                errorCounter.increment("provider", providerId.value(), "operation", "health_check");
                                log.error("Health check failed for provider: {}: {}", 
                                        providerId, error.getMessage());
                            });
                })
                .onErrorReturn(new ProviderHealthCheck(
                        providerId,
                        ProviderStatus.UNAVAILABLE,
                        "Provider gateway not found or unavailable",
                        0L,
                        Instant.now()
                ));
    }

    @Override
    public Mono<List<ModelInfo>> getAvailableModels(ProviderId providerId) {
        log.debug("Getting available models for provider: {}", providerId);
        
        return getProviderGateway(providerId)
                .flatMap(gateway -> {
                    requestCounter.increment("provider", providerId.value(), "operation", "list_models");
                    
                    return gateway.getAvailableModels(providerId)
                            .doOnSuccess(models -> {
                                log.debug("Retrieved {} models for provider: {}", 
                                        models.size(), providerId);
                            })
                            .doOnError(error -> {
                                errorCounter.increment("provider", providerId.value(), "operation", "list_models");
                                log.error("Failed to get models for provider: {}: {}", 
                                        providerId, error.getMessage());
                            });
                })
                .onErrorReturn(List.of());
    }

    @Override
    public Mono<Integer> estimateTokenCount(ProviderId providerId, ModelName model, PromptContent prompt) {
        log.debug("Estimating token count for provider: {} model: {}", providerId, model);
        
        return getProviderGateway(providerId)
                .flatMap(gateway -> {
                    requestCounter.increment("provider", providerId.value(), "operation", "token_estimate");
                    
                    return gateway.estimateTokenCount(providerId, model, prompt)
                            .doOnSuccess(count -> {
                                log.debug("Estimated {} tokens for provider: {} model: {}", 
                                        count, providerId, model);
                            })
                            .doOnError(error -> {
                                errorCounter.increment("provider", providerId.value(), "operation", "token_estimate");
                                log.error("Failed to estimate tokens for provider: {} model: {}: {}", 
                                        providerId, model, error.getMessage());
                            });
                })
                .onErrorReturn(0);
    }

    @Override
    public boolean supportsCapability(ProviderId providerId, LlmModel.ModelCapability capability) {
        LlmProviderGateway gateway = providerGateways.get(providerId);
        if (gateway == null) {
            log.warn("Provider gateway not found for provider: {}", providerId);
            return false;
        }
        
        boolean supports = gateway.supportsCapability(providerId, capability);
        log.debug("Provider: {} supports capability {}: {}", providerId, capability, supports);
        
        return supports;
    }

    /**
     * Get the provider gateway for the specified provider ID.
     */
    private Mono<LlmProviderGateway> getProviderGateway(ProviderId providerId) {
        LlmProviderGateway gateway = providerGateways.get(providerId);
        if (gateway == null) {
            String errorMessage = String.format("No provider gateway found for provider: %s. Available providers: %s", 
                    providerId, providerGateways.keySet());
            log.error(errorMessage);
            return Mono.error(new IllegalArgumentException(errorMessage));
        }
        
        return Mono.just(gateway);
    }

    /**
     * Register a new provider gateway.
     */
    public void registerProvider(ProviderId providerId, LlmProviderGateway gateway) {
        if (providerId == null || gateway == null) {
            throw new IllegalArgumentException("Provider ID and gateway cannot be null");
        }
        
        LlmProviderGateway existing = providerGateways.put(providerId, gateway);
        if (existing != null) {
            log.info("Replaced existing provider gateway for: {}", providerId);
        } else {
            log.info("Registered new provider gateway for: {}", providerId);
        }
    }

    /**
     * Unregister a provider gateway.
     */
    public void unregisterProvider(ProviderId providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID cannot be null");
        }
        
        LlmProviderGateway removed = providerGateways.remove(providerId);
        if (removed != null) {
            log.info("Unregistered provider gateway for: {}", providerId);
        } else {
            log.warn("No provider gateway found to unregister for: {}", providerId);
        }
    }

    /**
     * Get all registered provider IDs.
     */
    public List<ProviderId> getRegisteredProviders() {
        return List.copyOf(providerGateways.keySet());
    }

    /**
     * Check if a provider is registered.
     */
    public boolean isProviderRegistered(ProviderId providerId) {
        return providerGateways.containsKey(providerId);
    }

    /**
     * Get the total number of registered providers.
     */
    public int getProviderCount() {
        return providerGateways.size();
    }

    /**
     * Get health status for all registered providers.
     */
    public Mono<Map<ProviderId, ProviderHealthCheck>> getAllProviderHealth() {
        log.debug("Checking health for all {} registered providers", providerGateways.size());
        
        Map<ProviderId, Mono<ProviderHealthCheck>> healthChecks = new ConcurrentHashMap<>();
        
        providerGateways.keySet().forEach(providerId -> {
            healthChecks.put(providerId, checkProviderHealth(providerId));
        });
        
        return Flux.fromIterable(healthChecks.entrySet())
                .flatMap(entry -> 
                    entry.getValue()
                            .map(health -> Map.entry(entry.getKey(), health))
                            .onErrorReturn(Map.entry(entry.getKey(), 
                                    new ProviderHealthCheck(
                                            entry.getKey(),
                                            ProviderStatus.UNAVAILABLE,
                                            "Health check failed",
                                            0L,
                                            Instant.now()
                                    )))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .doOnSuccess(results -> {
                    log.debug("Completed health checks for {} providers", results.size());
                });
    }

    /**
     * Get available models for all registered providers.
     */
    public Mono<Map<ProviderId, List<ModelInfo>>> getAllProviderModels() {
        log.debug("Getting available models for all {} registered providers", providerGateways.size());
        
        Map<ProviderId, Mono<List<ModelInfo>>> modelRequests = new ConcurrentHashMap<>();
        
        providerGateways.keySet().forEach(providerId -> {
            modelRequests.put(providerId, getAvailableModels(providerId));
        });
        
        return Flux.fromIterable(modelRequests.entrySet())
                .flatMap(entry -> 
                    entry.getValue()
                            .map(models -> Map.entry(entry.getKey(), models))
                            .onErrorReturn(Map.entry(entry.getKey(), List.of()))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .doOnSuccess(results -> {
                    int totalModels = results.values().stream()
                            .mapToInt(List::size)
                            .sum();
                    log.debug("Retrieved models from {} providers, total: {} models", 
                            results.size(), totalModels);
                });
    }
}