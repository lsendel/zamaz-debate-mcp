package com.zamaz.mcp.organization.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for organization-user relationship.
 * This is part of the persistence adapter layer.
 */
@Entity
@Table(name = "organization_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OrganizationUserEntity.OrganizationUserId.class)
public class OrganizationUserEntity {
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "MEMBER";
    
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationUserEntity that = (OrganizationUserEntity) o;
        return Objects.equals(organization.getId(), that.organization.getId()) &&
               Objects.equals(user.getId(), that.user.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(
            organization != null ? organization.getId() : null,
            user != null ? user.getId() : null
        );
    }
    
    /**
     * Composite primary key for OrganizationUser entity.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationUserId implements Serializable {
        private UUID organization;
        private UUID user;
    }
}