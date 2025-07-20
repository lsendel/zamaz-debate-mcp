package com.zamaz.mcp.controller.config;

import com.zamaz.mcp.security.model.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * Permission evaluator for agentic flow operations with organization-level
 * isolation.
 */
@Component
@Slf4j
public class AgenticFlowPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String permissionString = permission.toString();
        log.debug("Evaluating permission: {} for user: {}", permissionString, authentication.getName());

        // Check if user has required role
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        switch (permissionString) {
            case "CREATE_AGENTIC_FLOW":
                return hasAnyRole(authorities, "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER");
            case "READ_AGENTIC_FLOW":
                return hasAnyRole(authorities, "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER", "ROLE_VIEWER");
            case "UPDATE_AGENTIC_FLOW":
                return hasAnyRole(authorities, "ROLE_ADMIN", "ROLE_MODERATOR");
            case "DELETE_AGENTIC_FLOW":
                return hasAnyRole(authorities, "ROLE_ADMIN");
            case "EXECUTE_AGENTIC_FLOW":
                return hasAnyRole(authorities, "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER");
            case "CONFIGURE_DEBATE_FLOW":
                return hasAnyRole(authorities, "ROLE_ADMIN", "ROLE_MODERATOR");
            default:
                return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Organization-level data isolation
        if ("ORGANIZATION".equals(targetType)) {
            return hasOrganizationAccess(authentication, targetId.toString());
        }

        return hasPermission(authentication, null, permission);
    }

    /**
     * Checks if the user has access to the specified organization.
     */
    private boolean hasOrganizationAccess(Authentication authentication, String organizationId) {
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            // Check if user belongs to the organization
            String userOrgId = userPrincipal.getOrganizationId();
            boolean hasAccess = organizationId.equals(userOrgId);

            log.debug("Organization access check: user org={}, requested org={}, access={}",
                    userOrgId, organizationId, hasAccess);

            return hasAccess;
        }

        return false;
    }

    /**
     * Checks if the user has any of the specified roles.
     */
    private boolean hasAnyRole(Collection<? extends GrantedAuthority> authorities, String... roles) {
        for (GrantedAuthority authority : authorities) {
            for (String role : roles) {
                if (role.equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}