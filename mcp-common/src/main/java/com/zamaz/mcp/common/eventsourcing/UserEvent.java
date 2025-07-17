package com.zamaz.mcp.common.eventsourcing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for user-related events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class UserEvent extends BaseEvent {
    
    public static final String AGGREGATE_TYPE = "user";
    
    // Event types
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_ACTIVATED = "user.activated";
    public static final String USER_DEACTIVATED = "user.deactivated";
    public static final String USER_LOGIN = "user.login";
    public static final String USER_LOGOUT = "user.logout";
    public static final String USER_PASSWORD_CHANGED = "user.password.changed";
    public static final String USER_PROFILE_UPDATED = "user.profile.updated";
    public static final String USER_ROLE_CHANGED = "user.role.changed";
    
    /**
     * Create a user created event
     */
    public static UserEvent created(String userId, String organizationId, String createdBy, Object payload) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_CREATED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(1L)
            .timestamp(LocalDateTime.now())
            .userId(createdBy)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a user updated event
     */
    public static UserEvent updated(String userId, String organizationId, String updatedBy, long version, Object payload) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_UPDATED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(updatedBy)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a user deleted event
     */
    public static UserEvent deleted(String userId, String organizationId, String deletedBy, long version) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_DELETED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(deletedBy)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create a user activated event
     */
    public static UserEvent activated(String userId, String organizationId, String activatedBy, long version) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_ACTIVATED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(activatedBy)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create a user deactivated event
     */
    public static UserEvent deactivated(String userId, String organizationId, String deactivatedBy, long version) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_DEACTIVATED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(deactivatedBy)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create a user login event
     */
    public static UserEvent login(String userId, String organizationId, long version, Object payload) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_LOGIN)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a user logout event
     */
    public static UserEvent logout(String userId, String organizationId, long version) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_LOGOUT)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create a user password changed event
     */
    public static UserEvent passwordChanged(String userId, String organizationId, long version) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_PASSWORD_CHANGED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create a user profile updated event
     */
    public static UserEvent profileUpdated(String userId, String organizationId, long version, Object payload) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_PROFILE_UPDATED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a user role changed event
     */
    public static UserEvent roleChanged(String userId, String organizationId, String changedBy, 
                                       String oldRole, String newRole, long version, Object payload) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(USER_ROLE_CHANGED)
            .aggregateId(userId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(changedBy)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "oldRole", oldRole,
                "newRole", newRole
            ))
            .build();
    }
}