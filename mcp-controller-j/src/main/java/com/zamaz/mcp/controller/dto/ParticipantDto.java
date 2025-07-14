package com.zamaz.mcp.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class ParticipantDto {
    
    private UUID id;
    private UUID debateId;
    private String name;
    private String type;
    private String provider;
    private String model;
    private String position;
    private JsonNode settings;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateParticipantRequest {
        @NotBlank(message = "Name is required")
        private String name;
        
        @NotBlank(message = "Type is required")
        @Pattern(regexp = "human|ai", message = "Type must be 'human' or 'ai'")
        private String type;
        
        private String provider; // Required if type is 'ai'
        private String model; // Required if type is 'ai'
        
        @Pattern(regexp = "for|against", message = "Position must be 'for' or 'against'")
        private String position;
        
        private JsonNode settings;
    }
}