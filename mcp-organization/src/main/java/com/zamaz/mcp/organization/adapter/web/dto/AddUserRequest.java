package com.zamaz.mcp.organization.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for adding a user to an organization.
 */
@Data
public class AddUserRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Role is required")
    @Pattern(regexp = "OWNER|ADMIN|MEMBER|GUEST", message = "Invalid role. Must be OWNER, ADMIN, MEMBER, or GUEST")
    private String role = "MEMBER";
}