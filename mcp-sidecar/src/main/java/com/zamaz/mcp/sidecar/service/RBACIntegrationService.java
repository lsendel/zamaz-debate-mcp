package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * RBAC Integration Service for MCP Sidecar
 * 
 * Integrates with the existing mcp-security service for role-based access control,
 * user management, and permission validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RBACIntegrationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${SECURITY_SERVICE_URL:http://localhost:8082}")
    private String securityServiceUrl;

    /**
     * Validate user permissions for a specific resource and action
     */
    public Mono<Boolean> validatePermission(String userId, String organizationId, String permission, String resource) {
        log.debug("Validating permission for user: {}, org: {}, permission: {}, resource: {}", 
                userId, organizationId, permission, resource);
        
        return webClientBuilder.build()
                .get()
                .uri(securityServiceUrl + "/api/v1/rbac/permissions/validate")
                .header("X-User-ID", userId)
                .header("X-Organization-ID", organizationId)
                .header("X-Permission", permission)
                .header("X-Resource", resource)
                .retrieve()
                .bodyToMono(PermissionValidationResponse.class)
                .map(response -> response.isAllowed())
                .doOnSuccess(allowed -> log.debug("Permission validation result: {}", allowed))
                .doOnError(error -> log.error("Permission validation failed", error))
                .onErrorReturn(false);
    }

    /**
     * Get user roles for a specific organization
     */
    public Mono<List<String>> getUserRoles(String userId, String organizationId) {
        log.debug("Getting user roles for user: {}, org: {}", userId, organizationId);
        
        return webClientBuilder.build()
                .get()
                .uri(securityServiceUrl + "/api/v1/rbac/users/{userId}/roles", userId)
                .header("X-Organization-ID", organizationId)
                .retrieve()
                .bodyToMono(UserRolesResponse.class)
                .map(response -> response.getRoles())
                .doOnSuccess(roles -> log.debug("User roles retrieved: {}", roles))
                .doOnError(error -> log.error("Failed to retrieve user roles", error))
                .onErrorReturn(List.of());
    }

    /**
     * Get user permissions for a specific organization
     */
    public Mono<List<String>> getUserPermissions(String userId, String organizationId) {
        log.debug("Getting user permissions for user: {}, org: {}", userId, organizationId);
        
        return webClientBuilder.build()
                .get()
                .uri(securityServiceUrl + "/api/v1/rbac/users/{userId}/permissions", userId)
                .header("X-Organization-ID", organizationId)
                .retrieve()
                .bodyToMono(UserPermissionsResponse.class)
                .map(response -> response.getPermissions())
                .doOnSuccess(permissions -> log.debug("User permissions retrieved: {}", permissions))
                .doOnError(error -> log.error("Failed to retrieve user permissions", error))
                .onErrorReturn(List.of());
    }

    /**
     * Validate user organization membership
     */
    public Mono<Boolean> validateOrganizationMembership(String userId, String organizationId) {
        log.debug("Validating organization membership for user: {}, org: {}", userId, organizationId);
        
        return webClientBuilder.build()
                .get()
                .uri(securityServiceUrl + "/api/v1/rbac/organizations/{organizationId}/members/{userId}", organizationId, userId)
                .retrieve()
                .bodyToMono(OrganizationMembershipResponse.class)
                .map(response -> response.isActive())
                .doOnSuccess(active -> log.debug("Organization membership validation result: {}", active))
                .doOnError(error -> log.error("Organization membership validation failed", error))
                .onErrorReturn(false);
    }

    /**
     * Get organization details
     */
    public Mono<OrganizationDetails> getOrganizationDetails(String organizationId) {
        log.debug("Getting organization details for: {}", organizationId);
        
        return webClientBuilder.build()
                .get()
                .uri(securityServiceUrl + "/api/v1/rbac/organizations/{organizationId}", organizationId)
                .retrieve()
                .bodyToMono(OrganizationDetails.class)
                .doOnSuccess(org -> log.debug("Organization details retrieved: {}", org.getName()))
                .doOnError(error -> log.error("Failed to retrieve organization details", error));
    }

    /**
     * Audit security event
     */
    public Mono<Void> auditSecurityEvent(String userId, String organizationId, String action, String resource, Map<String, Object> details) {
        log.debug("Auditing security event: user={}, org={}, action={}, resource={}", 
                userId, organizationId, action, resource);
        
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .userId(userId)
                .organizationId(organizationId)
                .action(action)
                .resource(resource)
                .details(details)
                .timestamp(java.time.Instant.now())
                .build();
        
        return webClientBuilder.build()
                .post()
                .uri(securityServiceUrl + "/api/v1/rbac/audit")
                .bodyValue(event)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Security event audited successfully"))
                .doOnError(error -> log.error("Failed to audit security event", error));
    }

    // Response DTOs
    public static class PermissionValidationResponse {
        private boolean allowed;
        private String reason;

        public boolean isAllowed() { return allowed; }
        public void setAllowed(boolean allowed) { this.allowed = allowed; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class UserRolesResponse {
        private List<String> roles;

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public static class UserPermissionsResponse {
        private List<String> permissions;

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }

    public static class OrganizationMembershipResponse {
        private boolean active;
        private String role;

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class OrganizationDetails {
        private String id;
        private String name;
        private boolean active;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class SecurityAuditEvent {
        private String userId;
        private String organizationId;
        private String action;
        private String resource;
        private Map<String, Object> details;
        private java.time.Instant timestamp;

        public static SecurityAuditEventBuilder builder() {
            return new SecurityAuditEventBuilder();
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        
        public java.time.Instant getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.Instant timestamp) { this.timestamp = timestamp; }

        public static class SecurityAuditEventBuilder {
            private String userId;
            private String organizationId;
            private String action;
            private String resource;
            private Map<String, Object> details;
            private java.time.Instant timestamp;

            public SecurityAuditEventBuilder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public SecurityAuditEventBuilder organizationId(String organizationId) {
                this.organizationId = organizationId;
                return this;
            }

            public SecurityAuditEventBuilder action(String action) {
                this.action = action;
                return this;
            }

            public SecurityAuditEventBuilder resource(String resource) {
                this.resource = resource;
                return this;
            }

            public SecurityAuditEventBuilder details(Map<String, Object> details) {
                this.details = details;
                return this;
            }

            public SecurityAuditEventBuilder timestamp(java.time.Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public SecurityAuditEvent build() {
                SecurityAuditEvent event = new SecurityAuditEvent();
                event.userId = this.userId;
                event.organizationId = this.organizationId;
                event.action = this.action;
                event.resource = this.resource;
                event.details = this.details;
                event.timestamp = this.timestamp;
                return event;
            }
        }
    }
}