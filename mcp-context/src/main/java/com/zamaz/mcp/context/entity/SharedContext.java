package com.zamaz.mcp.context.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a context sharing relationship between organizations or users.
 * Enables controlled access to contexts across organizational boundaries.
 */
@Entity
@Table(name = "shared_contexts", indexes = {
    @Index(name = "idx_shared_context_id", columnList = "context_id"),
    @Index(name = "idx_shared_target_org", columnList = "target_organization_id"),
    @Index(name = "idx_shared_target_user", columnList = "target_user_id"),
    @Index(name = "idx_shared_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SharedContext {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_id", nullable = false)
    private Context context;
    
    @Column(name = "source_organization_id", nullable = false)
    private UUID sourceOrganizationId;
    
    @Column(name = "target_organization_id")
    private UUID targetOrganizationId;
    
    @Column(name = "target_user_id")
    private UUID targetUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Permission permission = Permission.READ;
    
    @Column(name = "shared_by", nullable = false)
    private UUID sharedBy;
    
    @Column(name = "shared_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant sharedAt = Instant.now();
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Version
    private Long version;
    
    public enum Permission {
        READ,
        WRITE,
        ADMIN
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}