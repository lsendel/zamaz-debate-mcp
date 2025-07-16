package com.zamaz.mcp.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a user in the MCP system with organization-based permissions.
 * Supports multi-tenant architecture with organization-specific roles and permissions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpUser {
    
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    
    // Global roles and permissions
    private Set<Role> globalRoles = new HashSet<>();
    private Set<Permission> globalPermissions = new HashSet<>();
    
    // Organization-specific roles and permissions
    private Map<String, Set<Role>> organizationRoles = new HashMap<>();
    private Map<String, Set<Permission>> organizationPermissions = new HashMap<>();
    
    // Context-level permissions
    private Set<ContextPermission> contextPermissions = new HashSet<>();
    
    // Active organization context
    private String currentOrganizationId;
    
    /**
     * Get all organization IDs this user belongs to.
     */
    public Set<String> getOrganizationIds() {
        return organizationRoles.keySet();
    }
    
    /**
     * Check if user has a specific global role.
     */
    public boolean hasRole(String roleName) {
        return globalRoles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
    
    /**
     * Check if user has a specific role in an organization.
     */
    public boolean hasOrganizationRole(String organizationId, String roleName) {
        return organizationRoles.getOrDefault(organizationId, new HashSet<>())
                .stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
    
    /**
     * Get roles for a specific organization.
     */
    public Set<Role> getOrganizationRoles(String organizationId) {
        return organizationRoles.getOrDefault(organizationId, new HashSet<>());
    }
    
    /**
     * Get permissions for a specific organization.
     */
    public Set<Permission> getOrganizationPermissions(String organizationId) {
        return organizationPermissions.getOrDefault(organizationId, new HashSet<>());
    }
    
    /**
     * Add a role to an organization.
     */
    public void addOrganizationRole(String organizationId, Role role) {
        organizationRoles.computeIfAbsent(organizationId, k -> new HashSet<>()).add(role);
    }
    
    /**
     * Remove a role from an organization.
     */
    public void removeOrganizationRole(String organizationId, Role role) {
        Set<Role> roles = organizationRoles.get(organizationId);
        if (roles != null) {
            roles.remove(role);
            if (roles.isEmpty()) {
                organizationRoles.remove(organizationId);
            }
        }
    }
    
    /**
     * Add a permission to an organization.
     */
    public void addOrganizationPermission(String organizationId, Permission permission) {
        organizationPermissions.computeIfAbsent(organizationId, k -> new HashSet<>()).add(permission);
    }
    
    /**
     * Remove a permission from an organization.
     */
    public void removeOrganizationPermission(String organizationId, Permission permission) {
        Set<Permission> permissions = organizationPermissions.get(organizationId);
        if (permissions != null) {
            permissions.remove(permission);
            if (permissions.isEmpty()) {
                organizationPermissions.remove(organizationId);
            }
        }
    }
    
    /**
     * Get all permissions for a user (global + organization-specific).
     */
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(globalPermissions);
        
        // Add permissions from global roles
        globalRoles.forEach(role -> allPermissions.addAll(role.getPermissions()));
        
        // Add organization-specific permissions
        organizationPermissions.values().forEach(allPermissions::addAll);
        
        // Add permissions from organization roles
        organizationRoles.values().forEach(roles -> 
            roles.forEach(role -> allPermissions.addAll(role.getPermissions()))
        );
        
        return allPermissions;
    }
    
    /**
     * Get all permissions for a user in a specific organization.
     */
    public Set<Permission> getAllPermissions(String organizationId) {
        Set<Permission> allPermissions = new HashSet<>(globalPermissions);
        
        // Add permissions from global roles
        globalRoles.forEach(role -> allPermissions.addAll(role.getPermissions()));
        
        // Add organization-specific permissions
        allPermissions.addAll(getOrganizationPermissions(organizationId));
        
        // Add permissions from organization roles
        getOrganizationRoles(organizationId).forEach(role -> 
            allPermissions.addAll(role.getPermissions())
        );
        
        return allPermissions;
    }
    
    /**
     * Check if user has a specific permission globally or in current organization.
     */
    public boolean hasPermission(String permission) {
        return hasGlobalPermission(permission) || 
               (currentOrganizationId != null && hasOrganizationPermission(currentOrganizationId, permission));
    }
    
    /**
     * Check if user has a specific global permission.
     */
    public boolean hasGlobalPermission(String permission) {
        return getAllPermissions().stream()
                .anyMatch(p -> p.matches(permission));
    }
    
    /**
     * Check if user has a specific permission in an organization.
     */
    public boolean hasOrganizationPermission(String organizationId, String permission) {
        return getAllPermissions(organizationId).stream()
                .anyMatch(p -> p.matches(permission));
    }
    
    /**
     * Inner class for context-level permissions.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextPermission {
        private String contextId;
        private Permission permission;
        private String grantedBy;
        private java.time.LocalDateTime grantedAt;
        private java.time.LocalDateTime expiresAt;
        
        public boolean isExpired() {
            return expiresAt != null && java.time.LocalDateTime.now().isAfter(expiresAt);
        }
    }
}