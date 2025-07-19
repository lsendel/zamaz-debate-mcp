package com.zamaz.mcp.authserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_users")
@Data
public class OrganizationUser {

    @EmbeddedId
    private OrganizationUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String role = "member";

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Embeddable
    @Data
    public static class OrganizationUserId {
        @Column(name = "organization_id")
        private UUID organizationId;

        @Column(name = "user_id")
        private UUID userId;
    }
}