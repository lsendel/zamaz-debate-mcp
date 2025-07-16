package com.zamaz.mcp.context.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.IntArrayList;
import com.knuddels.jtokkit.api.ModelType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for counting tokens in text using tiktoken.
 */
@Service
@Slf4j
public class TokenCountingService {
    
    @Value("${app.context.token.model:gpt-4}")
    private String tokenModel;
    
    private Encoding encoding;
    
    @PostConstruct
    public void init() {
        try {
            // Map common model names to tiktoken model types
            ModelType modelType = switch (tokenModel.toLowerCase()) {
                case "gpt-4", "gpt-4-32k" -> ModelType.GPT_4;
                case "gpt-3.5-turbo", "gpt-3.5-turbo-16k" -> ModelType.GPT_3_5_TURBO;
                case "claude", "claude-2", "claude-3" -> ModelType.GPT_4; // Use GPT-4 encoding as approximation
                default -> ModelType.GPT_4;
            };
            
            encoding = Encodings.newDefaultEncodingRegistry().getEncodingForModel(modelType);
            log.info("Initialized token counting with model: {} (using encoding: {})", tokenModel, modelType);
        } catch (Exception e) {
            log.error("Failed to initialize encoding for model: {}, using default", tokenModel, e);
            encoding = Encodings.newDefaultEncodingRegistry().getEncodingForModel(ModelType.GPT_4);
        }
    }
    
    /**
     * Count tokens in a text string.
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        try {
            IntArrayList tokens = encoding.encode(text);
            return tokens.size();
        } catch (Exception e) {
            log.error("Error counting tokens, using character-based estimation", e);
            // Fallback to character-based estimation (1 token ≈ 4 characters)
            return (int) Math.ceil(text.length() / 4.0);
        }
    }
    
    /**
     * Count tokens for a message with role.
     * Accounts for special tokens used in chat format.
     */
    public int countMessageTokens(String role, String content) {
        // Account for special tokens in chat format
        // <|im_start|>role\ncontent<|im_end|>\n
        int specialTokens = 4; // Approximate special token overhead
        
        return specialTokens + countTokens(role) + countTokens(content);
    }
    
    /**
     * Truncate text to fit within a token limit.
     */
    public String truncateToTokenLimit(String text, int maxTokens) {
        if (text == null || text.isEmpty() || maxTokens <= 0) {
            return "";
        }
        
        try {
            IntArrayList tokens = encoding.encode(text);
            
            if (tokens.size() <= maxTokens) {
                return text;
            }
            
            // Take only the tokens that fit
            IntArrayList truncatedTokens = new IntArrayList();
            for (int i = 0; i < maxTokens; i++) {
                truncatedTokens.add(tokens.get(i));
            }
            return encoding.decode(truncatedTokens);
        } catch (Exception e) {
            log.error("Error truncating text, using character-based truncation", e);
            // Fallback to character-based truncation
            int maxChars = maxTokens * 4;
            return text.length() <= maxChars ? text : text.substring(0, maxChars);
        }
    }
    
    /**
     * Estimate tokens for a list of messages.
     */
    public int estimateConversationTokens(List<MessageEstimate> messages) {
        return messages.stream()
                .mapToInt(msg -> countMessageTokens(msg.role(), msg.content()))
                .sum();
    }
    
    public record MessageEstimate(String role, String content) {}
}