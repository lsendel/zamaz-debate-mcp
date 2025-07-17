package com.zamaz.mcp.organization.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for organization details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    
    private UUID id;
    private String name;
    private String description;
    private Map<String, Object> settings;
    private boolean active;
    private int memberCount;
    private List<MemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Nested DTO for organization members.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResponse {
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private String role;
        private LocalDateTime joinedAt;
    }
}