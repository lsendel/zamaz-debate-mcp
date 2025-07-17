package com.zamaz.mcp.organization.adapter.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for updating an organization.
 * All fields are optional - only provided fields will be updated.
 */
@Data
public class UpdateOrganizationRequest {
    
    @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private Map<String, Object> settings;
}