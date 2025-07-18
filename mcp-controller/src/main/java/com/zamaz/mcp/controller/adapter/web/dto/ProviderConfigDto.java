package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for LLM provider configuration.
 */
@Schema(description = "Configuration for an LLM provider")
public record ProviderConfigDto(
    
    @Schema(description = "Model name", example = "claude-3-5-sonnet-20241022")
    @NotBlank(message = "Model cannot be blank")
    String model,
    
    @Schema(description = "Maximum tokens to generate", example = "4096")
    @NotNull(message = "Max tokens cannot be null")
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 32768, message = "Max tokens cannot exceed 32,768")
    Integer maxTokens,
    
    @Schema(description = "Temperature for randomness (0.0-2.0)", example = "0.7")
    @NotNull(message = "Temperature cannot be null")
    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "2.0", message = "Temperature cannot exceed 2.0")
    BigDecimal temperature,
    
    @Schema(description = "Top-p for nucleus sampling (0.1-1.0)", example = "0.95")
    @NotNull(message = "TopP cannot be null")
    @DecimalMin(value = "0.1", message = "TopP must be at least 0.1")
    @DecimalMax(value = "1.0", message = "TopP cannot exceed 1.0")
    BigDecimal topP,
    
    @Schema(description = "System prompt for the AI", example = "You are participating in a debate...")
    @NotNull(message = "System prompt cannot be null")
    @Size(max = 10000, message = "System prompt cannot exceed 10,000 characters")
    String systemPrompt,
    
    @Schema(description = "Additional parameters")
    Map<String, Object> additionalParams
) {
    
    public static ProviderConfigDto forClaude(String systemPrompt) {
        return new ProviderConfigDto(
            "claude-3-5-sonnet-20241022",
            4096,
            BigDecimal.valueOf(0.7),
            BigDecimal.valueOf(0.95),
            systemPrompt,
            Map.of()
        );
    }
    
    public static ProviderConfigDto forOpenAI(String systemPrompt) {
        return new ProviderConfigDto(
            "gpt-4o",
            4096,
            BigDecimal.valueOf(0.7),
            BigDecimal.valueOf(0.95),
            systemPrompt,
            Map.of()
        );
    }
    
    public static ProviderConfigDto forGemini(String systemPrompt) {
        return new ProviderConfigDto(
            "gemini-1.5-pro",
            8192,
            BigDecimal.valueOf(0.9),
            BigDecimal.valueOf(0.95),
            systemPrompt,
            Map.of()
        );
    }
}