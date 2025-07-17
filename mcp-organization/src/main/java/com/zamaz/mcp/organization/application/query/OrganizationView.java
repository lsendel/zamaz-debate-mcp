package com.zamaz.mcp.organization.application.query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read model representing an organization for query responses.
 * This is a DTO that contains the data needed for presentation.
 */
public record OrganizationView(
    UUID id,
    String name,
    String description,
    Map<String, Object> settings,
    boolean active,
    int memberCount,
    List<MemberView> members,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * Nested view for organization members.
     */
    public record MemberView(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime joinedAt
    ) {
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }
}