package com.zamaz.mcp.common.eventsourcing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for organization-related events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OrganizationEvent extends BaseEvent {
    
    public static final String AGGREGATE_TYPE = "organization";
    
    // Event types
    public static final String ORGANIZATION_CREATED = "organization.created";
    public static final String ORGANIZATION_UPDATED = "organization.updated";
    public static final String ORGANIZATION_DELETED = "organization.deleted";
    public static final String ORGANIZATION_SETTINGS_UPDATED = "organization.settings.updated";
    public static final String ORGANIZATION_USER_ADDED = "organization.user.added";
    public static final String ORGANIZATION_USER_REMOVED = "organization.user.removed";
    public static final String ORGANIZATION_USER_ROLE_CHANGED = "organization.user.role.changed";
    
    /**
     * Create an organization created event
     */
    public static OrganizationEvent created(String organizationId, String userId, Object payload) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_CREATED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(1L)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create an organization updated event
     */
    public static OrganizationEvent updated(String organizationId, String userId, long version, Object payload) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_UPDATED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create an organization deleted event
     */
    public static OrganizationEvent deleted(String organizationId, String userId, long version) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_DELETED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create an organization settings updated event
     */
    public static OrganizationEvent settingsUpdated(String organizationId, String userId, long version, Object payload) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_SETTINGS_UPDATED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a user added to organization event
     */
    public static OrganizationEvent userAdded(String organizationId, String userId, String addedUserId, long version, Object payload) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_USER_ADDED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("addedUserId", addedUserId))
            .build();
    }
    
    /**
     * Create a user removed from organization event
     */
    public static OrganizationEvent userRemoved(String organizationId, String userId, String removedUserId, long version, Object payload) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_USER_REMOVED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("removedUserId", removedUserId))
            .build();
    }
    
    /**
     * Create a user role changed event
     */
    public static OrganizationEvent userRoleChanged(String organizationId, String userId, String targetUserId, 
                                                   String oldRole, String newRole, long version, Object payload) {
        return OrganizationEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ORGANIZATION_USER_ROLE_CHANGED)
            .aggregateId(organizationId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "targetUserId", targetUserId,
                "oldRole", oldRole,
                "newRole", newRole
            ))
            .build();
    }
}