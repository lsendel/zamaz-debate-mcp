package com.zamaz.mcp.context.adapter.external;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.zamaz.mcp.common.architecture.adapter.external.ExternalServiceAdapter;
import com.zamaz.mcp.context.application.port.outbound.TokenCountingService;
import com.zamaz.mcp.context.domain.model.MessageContent;
import com.zamaz.mcp.context.domain.model.TokenCount;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Token counting service implementation using Jtokkit library.
 * This is an external service adapter in hexagonal architecture.
 */
@Component
public class JtokkitTokenCountingService implements TokenCountingService, ExternalServiceAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(JtokkitTokenCountingService.class);
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4;
    
    private final EncodingRegistry registry;
    private final Map<String, ModelType> modelMapping;
    
    public JtokkitTokenCountingService() {
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.modelMapping = initializeModelMapping();
    }
    
    @Override
    public TokenCount countTokens(MessageContent content, String model) {
        try {
            ModelType modelType = getModelType(model);
            Encoding encoding = registry.getEncodingForModel(modelType);
            int tokenCount = encoding.countTokens(content.value());
            
            logger.debug("Counted {} tokens for {} characters using model {}", 
                tokenCount, content.length(), model);
            
            return TokenCount.of(tokenCount);
        } catch (Exception e) {
            logger.warn("Failed to count tokens using model {}, falling back to estimation: {}", 
                model, e.getMessage());
            return estimateTokens(content);
        }
    }
    
    @Override
    public TokenCount countTokens(MessageContent content) {
        return countTokens(content, DEFAULT_MODEL);
    }
    
    @Override
    public MessageContent truncateToTokenLimit(MessageContent content, TokenCount maxTokens, String model) {
        try {
            ModelType modelType = getModelType(model);
            Encoding encoding = registry.getEncodingForModel(modelType);
            
            // Encode to tokens
            var tokens = encoding.encode(content.value());
            
            // If within limit, return as is
            if (tokens.size() <= maxTokens.value()) {
                return content;
            }
            
            // Truncate tokens and decode back to string
            var truncatedTokens = tokens.subList(0, maxTokens.value());
            String truncatedText = encoding.decode(truncatedTokens);
            
            logger.debug("Truncated content from {} to {} tokens", 
                tokens.size(), maxTokens.value());
            
            return MessageContent.of(truncatedText);
        } catch (Exception e) {
            logger.warn("Failed to truncate using model {}, falling back to character-based truncation: {}", 
                model, e.getMessage());
            
            // Fallback to character-based truncation
            int estimatedMaxChars = maxTokens.value() * CHARS_PER_TOKEN_ESTIMATE;
            return content.truncate(estimatedMaxChars);
        }
    }
    
    @Override
    public TokenCount estimateTokens(MessageContent content) {
        // Simple estimation: roughly 1 token per 4 characters
        int estimatedTokens = Math.max(1, content.length() / CHARS_PER_TOKEN_ESTIMATE);
        
        logger.debug("Estimated {} tokens for {} characters", 
            estimatedTokens, content.length());
        
        return TokenCount.of(estimatedTokens);
    }
    
    @Override
    public boolean isModelSupported(String model) {
        return modelMapping.containsKey(model.toLowerCase());
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    private ModelType getModelType(String model) {
        if (model == null) {
            return ModelType.GPT_3_5_TURBO;
        }
        
        ModelType modelType = modelMapping.get(model.toLowerCase());
        if (modelType == null) {
            logger.warn("Unknown model '{}', using default model type", model);
            return ModelType.GPT_3_5_TURBO;
        }
        
        return modelType;
    }
    
    private Map<String, ModelType> initializeModelMapping() {
        Map<String, ModelType> mapping = new HashMap<>();
        
        // OpenAI models
        mapping.put("gpt-4", ModelType.GPT_4);
        mapping.put("gpt-4-32k", ModelType.GPT_4_32K);
        mapping.put("gpt-3.5-turbo", ModelType.GPT_3_5_TURBO);
        mapping.put("gpt-3.5-turbo-16k", ModelType.GPT_3_5_TURBO_16K);
        
        // Claude models (map to similar token counts)
        mapping.put("claude-3-opus", ModelType.GPT_4);
        mapping.put("claude-3-sonnet", ModelType.GPT_4);
        mapping.put("claude-3-haiku", ModelType.GPT_3_5_TURBO);
        mapping.put("claude-2.1", ModelType.GPT_4);
        mapping.put("claude-2", ModelType.GPT_4);
        
        // Other models
        mapping.put("llama-2-70b", ModelType.GPT_4);
        mapping.put("llama-2-13b", ModelType.GPT_3_5_TURBO);
        mapping.put("llama-2-7b", ModelType.GPT_3_5_TURBO);
        
        return mapping;
    }
}