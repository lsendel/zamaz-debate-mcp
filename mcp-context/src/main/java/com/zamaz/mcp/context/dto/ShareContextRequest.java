package com.zamaz.mcp.context.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for sharing a context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareContextRequest {
    
    private UUID targetOrganizationId;
    
    private UUID targetUserId;
    
    @NotNull(message = "Permission level is required")
    private String permission;
    
    @Future(message = "Expiration date must be in the future")
    private Instant expiresAt;
}