package com.zamaz.mcp.common.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for all domain entities in hexagonal architecture.
 * This is a pure domain class with no framework dependencies.
 * Provides common functionality for entity identity management.
 * 
 * @param <ID> The type of the entity identifier
 */
public abstract class DomainEntity<ID> {
    
    protected final ID id;
    protected final LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    
    protected DomainEntity(ID id) {
        this.id = Objects.requireNonNull(id, "Entity ID cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    public ID getId() {
        return id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Updates the entity's timestamp.
     * Should be called when entity state changes.
     */
    protected void markAsModified() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DomainEntity<?> that = (DomainEntity<?>) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    /**
     * Checks if this entity is the same as another entity based on identity.
     * 
     * @param other the other entity
     * @return true if both entities have the same identity
     */
    public boolean sameIdentityAs(DomainEntity<ID> other) {
        return other != null && Objects.equals(this.id, other.id);
    }
}