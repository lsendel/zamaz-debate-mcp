package com.zamaz.mcp.llm.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to generate a streaming text completion using an LLM provider.
 * Similar to GenerateCompletionCommand but includes streaming-specific options.
 */
public record StreamCompletionCommand(
    String prompt,
    Optional<String> preferredModel,
    Optional<String> preferredProvider,
    int maxTokens,
    double temperature,
    boolean enableCaching,
    String organizationId,
    String userId,
    // Streaming-specific fields
    boolean enableDelta,
    Optional<String> streamId,
    int bufferSize
) implements Command {
    
    public StreamCompletionCommand {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(preferredModel, "Preferred model cannot be null");
        Objects.requireNonNull(preferredProvider, "Preferred provider cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(streamId, "Stream ID cannot be null");
        
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
        
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
    }
    
    public static StreamCompletionCommand of(
            String prompt,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new StreamCompletionCommand(
            prompt,
            Optional.empty(),
            Optional.empty(),
            maxTokens,
            0.7, // Default temperature
            true, // Default enable caching
            organizationId,
            userId,
            true, // Default enable delta
            Optional.empty(), // No specific stream ID
            1024 // Default buffer size
        );
    }
    
    public static StreamCompletionCommand withModel(
            String prompt,
            String model,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new StreamCompletionCommand(
            prompt,
            Optional.of(model),
            Optional.empty(),
            maxTokens,
            0.7,
            true,
            organizationId,
            userId,
            true,
            Optional.empty(),
            1024
        );
    }
    
    public static StreamCompletionCommand withProvider(
            String prompt,
            String provider,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new StreamCompletionCommand(
            prompt,
            Optional.empty(),
            Optional.of(provider),
            maxTokens,
            0.7,
            true,
            organizationId,
            userId,
            true,
            Optional.empty(),
            1024
        );
    }
    
    public static StreamCompletionCommand withStreamId(
            String prompt,
            String streamId,
            int maxTokens,
            String organizationId,
            String userId
    ) {
        return new StreamCompletionCommand(
            prompt,
            Optional.empty(),
            Optional.empty(),
            maxTokens,
            0.7,
            true,
            organizationId,
            userId,
            true,
            Optional.of(streamId),
            1024
        );
    }
    
    public static StreamCompletionCommand withCustomBuffer(
            String prompt,
            int maxTokens,
            int bufferSize,
            String organizationId,
            String userId
    ) {
        return new StreamCompletionCommand(
            prompt,
            Optional.empty(),
            Optional.empty(),
            maxTokens,
            0.7,
            true,
            organizationId,
            userId,
            true,
            Optional.empty(),
            bufferSize
        );
    }
}