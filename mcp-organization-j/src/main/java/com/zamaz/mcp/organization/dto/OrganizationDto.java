package com.zamaz.mcp.organization.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class OrganizationDto {
    
    private UUID id;
    
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private JsonNode settings;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Boolean isActive;
    
    private Integer userCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrganizationRequest {
        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
        private String name;
        
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;
        
        private JsonNode settings;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateOrganizationRequest {
        @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
        private String name;
        
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;
        
        private JsonNode settings;
        
        private Boolean isActive;
    }
}