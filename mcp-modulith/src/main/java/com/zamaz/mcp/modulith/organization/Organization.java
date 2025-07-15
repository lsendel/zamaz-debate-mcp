package com.zamaz.mcp.modulith.organization;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Organization entity representing a tenant in the multi-tenant system.
 */
@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;
    
    @ElementCollection
    @CollectionTable(name = "organization_features", joinColumns = @JoinColumn(name = "organization_id"))
    @Column(name = "feature")
    private Set<String> enabledFeatures = new HashSet<>();
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrganizationStatus.ACTIVE;
        }
        if (tier == null) {
            tier = SubscriptionTier.FREE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum OrganizationStatus {
        ACTIVE,
        SUSPENDED,
        INACTIVE,
        PENDING_APPROVAL
    }
    
    public enum SubscriptionTier {
        FREE,
        BASIC,
        PROFESSIONAL,
        ENTERPRISE
    }
}