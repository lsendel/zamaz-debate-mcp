package com.zamaz.mcp.llm.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to generate a text completion using an LLM provider.
 */
public record GenerateCompletionCommand(
    String prompt,
    Optional<String> preferredModel,
    Optional<String> preferredProvider,
    int maxTokens,
    double temperature,
    boolean enableCaching,
    String organizationId,
    String userId
) implements Command {
    
    public GenerateCompletionCommand {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(preferredModel, "Preferred model cannot be null");
        Objects.requireNonNull(preferredProvider, "Preferred provider cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        
        if (prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }
        
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
        
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }
    
    public static GenerateCompletionCommand of(
            String prompt,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new GenerateCompletionCommand(
            prompt,
            Optional.empty(),
            Optional.empty(),
            maxTokens,
            0.7, // Default temperature
            true, // Default enable caching
            organizationId,
            userId
        );
    }
    
    public static GenerateCompletionCommand withModel(
            String prompt,
            String model,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new GenerateCompletionCommand(
            prompt,
            Optional.of(model),
            Optional.empty(),
            maxTokens,
            0.7,
            true,
            organizationId,
            userId
        );
    }
    
    public static GenerateCompletionCommand withProvider(
            String prompt,
            String provider,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new GenerateCompletionCommand(
            prompt,
            Optional.empty(),
            Optional.of(provider),
            maxTokens,
            0.7,
            true,
            organizationId,
            userId
        );
    }
}