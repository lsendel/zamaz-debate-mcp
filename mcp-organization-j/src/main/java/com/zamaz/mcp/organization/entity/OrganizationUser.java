package com.zamaz.mcp.organization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OrganizationUser.OrganizationUserId.class)
public class OrganizationUser {
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "member";
    
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationUserId implements Serializable {
        private UUID organization;
        private UUID user;
    }
}