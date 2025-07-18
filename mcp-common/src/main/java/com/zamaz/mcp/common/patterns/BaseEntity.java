package com.zamaz.mcp.common.patterns;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity class providing common fields and functionality for all entities.
 * Includes audit fields, soft delete support, and common patterns.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Generate a new UUID for the entity ID.
     */
    @PrePersist
    protected void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * Set audit fields before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
        // Set created_by from security context if available
        if (this.createdBy == null) {
            this.createdBy = getCurrentUser();
        }
        if (this.updatedBy == null) {
            this.updatedBy = this.createdBy;
        }
    }

    /**
     * Set audit fields before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // Set updated_by from security context if available
        this.updatedBy = getCurrentUser();
    }

    /**
     * Soft delete the entity.
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = getCurrentUser();
    }

    /**
     * Restore a soft-deleted entity.
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /**
     * Check if the entity is active (not deleted).
     */
    public boolean isActive() {
        return !deleted;
    }

    /**
     * Check if the entity is new (not persisted yet).
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Get the current user from security context.
     * This is a placeholder implementation - should be overridden or injected.
     */
    private String getCurrentUser() {
        // In a real application, this would extract the current user from
        // the security context (e.g., Spring Security)
        return "system";
    }

    /**
     * Get a display name for the entity.
     * Override in subclasses to provide meaningful names.
     */
    public String getDisplayName() {
        return this.getClass().getSimpleName() + ":" + this.id;
    }

    /**
     * Get entity metadata for logging and debugging.
     */
    public String getEntityMetadata() {
        return String.format("%s[id=%s, organizationId=%s, version=%d, deleted=%b]",
            this.getClass().getSimpleName(), this.id, this.organizationId, this.version, this.deleted);
    }
}