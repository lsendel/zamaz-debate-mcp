package com.zamaz.mcp.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto {
    
    private UUID id;
    private UUID roundId;
    private UUID participantId;
    private String participantName;
    private String content;
    private Integer tokenCount;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponseRequest {
        @NotNull(message = "Participant ID is required")
        private UUID participantId;
        
        @NotBlank(message = "Content is required")
        private String content;
    }
}