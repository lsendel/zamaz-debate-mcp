package com.zamaz.mcp.llm.cache;

import com.zamaz.mcp.llm.model.CompletionRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Generates cache keys for LLM requests.
 */
@Component
public class LlmCacheKeyGenerator {

    /**
     * Generate a cache key for a completion request.
     *
     * @param request the completion request
     * @return the cache key
     */
    public String generateKey(CompletionRequest request) {
        // Include all relevant fields that affect the response
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("model:").append(request.getModel()).append(":");
        keyBuilder.append("prompt:").append(request.getPrompt()).append(":");
        
        if (request.getSystemPrompt() != null) {
            keyBuilder.append("system:").append(request.getSystemPrompt()).append(":");
        }
        
        if (request.getMaxTokens() != null) {
            keyBuilder.append("maxTokens:").append(request.getMaxTokens()).append(":");
        }
        
        if (request.getTemperature() != null) {
            keyBuilder.append("temp:").append(request.getTemperature()).append(":");
        }
        
        if (request.getTopP() != null) {
            keyBuilder.append("topP:").append(request.getTopP()).append(":");
        }
        
        if (request.getStopSequences() != null && !request.getStopSequences().isEmpty()) {
            keyBuilder.append("stop:").append(String.join(",", request.getStopSequences())).append(":");
        }
        
        // Hash the key to keep it a reasonable length and avoid special characters
        return hashString(keyBuilder.toString());
    }
    
    /**
     * Hash a string using SHA-256 and encode it as Base64.
     *
     * @param input the input string
     * @return the hashed string
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash cache key", e);
        }
    }
}
