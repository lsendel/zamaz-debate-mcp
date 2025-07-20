package com.zamaz.workflow.application.dto;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
public class CreateWorkflowRequest {
    @NotBlank(message = "Workflow name is required")
    @Size(min = 3, max = 100, message = "Workflow name must be between 3 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotBlank(message = "Organization ID is required")
    private String organizationId;
    
    @NotBlank(message = "Created by is required")
    private String createdBy;
}