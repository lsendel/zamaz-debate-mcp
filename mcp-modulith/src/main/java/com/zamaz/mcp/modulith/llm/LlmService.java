package com.zamaz.mcp.modulith.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interacting with LLM providers.
 * Acts as a gateway to different LLM implementations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {
    
    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<>();
    
    /**
     * Registers an LLM provider.
     */
    public void registerProvider(String name, LlmProvider provider) {
        log.info("Registering LLM provider: {}", name);
        providers.put(name.toLowerCase(), provider);
    }
    
    /**
     * Generates a response using the specified provider.
     */
    public String generateResponse(String providerName, String systemPrompt, String userPrompt) {
        log.debug("Generating response with provider: {}", providerName);
        
        LlmProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // Fallback to mock provider for demo
            log.warn("Provider {} not found, using mock provider", providerName);
            return generateMockResponse(providerName, systemPrompt, userPrompt);
        }
        
        return provider.generateResponse(systemPrompt, userPrompt);
    }
    
    /**
     * Mock response generator for demo purposes.
     */
    private String generateMockResponse(String provider, String systemPrompt, String userPrompt) {
        return String.format(
            "[%s Response] Based on the topic '%s', I believe that... (mock response)",
            provider,
            extractTopic(userPrompt)
        );
    }
    
    private String extractTopic(String prompt) {
        // Simple topic extraction
        if (prompt.contains("Topic:")) {
            int start = prompt.indexOf("Topic:") + 6;
            int end = prompt.indexOf("\n", start);
            if (end > start) {
                return prompt.substring(start, end).trim();
            }
        }
        return "the given subject";
    }
    
    /**
     * Interface for LLM providers.
     */
    public interface LlmProvider {
        String generateResponse(String systemPrompt, String userPrompt);
    }
}