package com.zamaz.mcp.llm.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionRequest {
    
    @NotBlank(message = "Provider is required")
    private String provider;
    
    private String model;
    
    @NotNull(message = "Messages are required")
    private List<Message> messages;
    
    @Min(1)
    @Max(8192)
    private Integer maxTokens;
    
    @Min(0.0)
    @Max(2.0)
    private Double temperature;
    
    @Min(0.0)
    @Max(1.0)
    private Double topP;
    
    private Integer topK;
    
    private Boolean stream;
    
    private String systemPrompt;
    
    private List<String> stopSequences;
    
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        @NotBlank(message = "Role is required")
        private String role; // system, user, assistant
        
        @NotBlank(message = "Content is required")
        private String content;
        
        private String name;
    }
}