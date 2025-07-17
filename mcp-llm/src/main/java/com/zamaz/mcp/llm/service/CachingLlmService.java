package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.common.cache.CacheService;
import com.zamaz.mcp.llm.cache.LlmCacheKeyGenerator;
import com.zamaz.mcp.llm.config.LlmCacheConfig;
import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import com.zamaz.mcp.llm.model.StreamingCompletionRequest;
import com.zamaz.mcp.llm.model.TokenCountRequest;
import com.zamaz.mcp.llm.model.TokenCountResponse;
import com.zamaz.mcp.llm.provider.LlmProvider;
import com.zamaz.mcp.llm.provider.LlmProviderFactory;
import com.zamaz.mcp.llm.provider.ModelInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * LLM service implementation with caching support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachingLlmService implements LlmService {

    private final LlmProviderFactory providerFactory;
    private final CacheService<String, CompletionResponse> completionCache;
    private final LlmCacheKeyGenerator cacheKeyGenerator;
    private final LlmCacheConfig cacheConfig;

    /**
     * Generate a completion for the given request.
     *
     * @param request the completion request
     * @return the completion response
     */
    @Override
    public CompletionResponse complete(CompletionRequest request) {
        // Don't cache if streaming is enabled
        if (Boolean.TRUE.equals(request.getStream())) {
            return getProvider(request.getModel()).complete(request);
        }

        // Generate cache key
        String cacheKey = cacheKeyGenerator.generateKey(request);
        Duration ttl = cacheConfig.getTtlForModel(request.getModel());

        // Get from cache or compute
        return completionCache.getOrCompute(cacheKey, () -> {
            log.debug("Cache miss for model: {}, generating completion", request.getModel());
            return getProvider(request.getModel()).complete(request);
        }, ttl);
    }

    /**
     * Stream a completion for the given request.
     *
     * @param request the streaming completion request
     * @return a flux of completion responses
     */
    @Override
    public Flux<CompletionResponse> streamComplete(StreamingCompletionRequest request) {
        // Streaming responses are not cached
        return getProvider(request.getModel()).streamComplete(request);
    }

    /**
     * Count tokens in the given text.
     *
     * @param request the token count request
     * @return the token count response
     */
    @Override
    public TokenCountResponse countTokens(TokenCountRequest request) {
        return getProvider(request.getModel()).countTokens(request);
    }

    /**
     * List available models.
     *
     * @return the list of available models
     */
    @Override
    public List<ModelInfo> listModels() {
        return providerFactory.listModels();
    }

    /**
     * Get the provider for the given model.
     *
     * @param model the model name
     * @return the LLM provider
     */
    private LlmProvider getProvider(String model) {
        return providerFactory.getProvider(model);
    }
}
