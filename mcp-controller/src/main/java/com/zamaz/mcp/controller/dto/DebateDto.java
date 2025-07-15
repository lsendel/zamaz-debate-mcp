package com.zamaz.mcp.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateDto {
    
    private UUID id;
    private UUID organizationId;
    private String title;
    private String description;
    private String topic;
    private String format;
    private Integer maxRounds;
    private Integer currentRound;
    private String status;
    private JsonNode settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<ParticipantDto> participants;
    private Integer participantCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDebateRequest {
        @NotNull(message = "Organization ID is required")
        private UUID organizationId;
        
        @NotBlank(message = "Title is required")
        private String title;
        
        private String description;
        
        @NotBlank(message = "Topic is required")
        private String topic;
        
        @NotBlank(message = "Format is required")
        private String format;
        
        @Min(value = 1, message = "Max rounds must be at least 1")
        private Integer maxRounds;
        
        private JsonNode settings;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDebateRequest {
        private String title;
        private String description;
        private String topic;
        private Integer maxRounds;
        private JsonNode settings;
    }
}