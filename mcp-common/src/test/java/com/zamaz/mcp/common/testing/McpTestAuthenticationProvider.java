package com.zamaz.mcp.common.testing;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Authentication provider for MCP testing.
 * Creates test authentication contexts with various roles and organization memberships.
 */
@Component
public class McpTestAuthenticationProvider {

    /**
     * Create test authentication for a regular user.
     */
    public Authentication createUserAuthentication() {
        return createAuthentication("test-user", "ROLE_USER", "TIER_FREE", "ORG_" + UUID.randomUUID());
    }

    /**
     * Create test authentication for an admin user.
     */
    public Authentication createAdminAuthentication() {
        return createAuthentication("test-admin", "ROLE_ADMIN", "TIER_ENTERPRISE", "ORG_" + UUID.randomUUID());
    }

    /**
     * Create test authentication for a pro tier user.
     */
    public Authentication createProUserAuthentication() {
        return createAuthentication("test-pro-user", "ROLE_USER", "TIER_PRO", "ORG_" + UUID.randomUUID());
    }

    /**
     * Create test authentication for a specific organization.
     */
    public Authentication createOrganizationAuthentication(String organizationId) {
        return createAuthentication("test-user", "ROLE_USER", "TIER_FREE", "ORG_" + organizationId);
    }

    /**
     * Create test authentication with custom parameters.
     */
    public Authentication createAuthentication(String username, String role, String tier, String organizationAuthority) {
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority(role),
            new SimpleGrantedAuthority(tier),
            new SimpleGrantedAuthority(organizationAuthority),
            new SimpleGrantedAuthority("ORG_TIER_" + tier.substring(5)) // Remove "TIER_" prefix
        );

        return new UsernamePasswordAuthenticationToken(username, "test-password", authorities);
    }

    /**
     * Create authentication for anonymous access (no auth).
     */
    public Authentication createAnonymousAuthentication() {
        return null; // Represents unauthenticated requests
    }

    /**
     * Extract organization ID from authentication.
     */
    public String extractOrganizationId(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        return authentication.getAuthorities().stream()
            .filter(auth -> auth.getAuthority().startsWith("ORG_"))
            .findFirst()
            .map(orgAuth -> orgAuth.getAuthority().substring(4)) // Remove "ORG_" prefix
            .orElse(null);
    }

    /**
     * Extract user tier from authentication.
     */
    public String extractUserTier(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "free";
        }

        return authentication.getAuthorities().stream()
            .filter(auth -> auth.getAuthority().startsWith("TIER_"))
            .findFirst()
            .map(tierAuth -> tierAuth.getAuthority().substring(5).toLowerCase()) // Remove "TIER_" prefix
            .orElse("free");
    }

    /**
     * Check if authentication has specific role.
     */
    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals(role));
    }

    /**
     * Create authentication with JWT-like structure for testing.
     */
    public Authentication createJwtLikeAuthentication(String userId, String organizationId, String tier) {
        String orgAuthority = "ORG_" + organizationId;
        String tierAuthority = "TIER_" + tier.toUpperCase();
        String orgTierAuthority = "ORG_TIER_" + tier.toUpperCase();

        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority(tierAuthority),
            new SimpleGrantedAuthority(orgAuthority),
            new SimpleGrantedAuthority(orgTierAuthority)
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userId, "jwt-token", authorities);
        
        // In a real JWT implementation, the token would be stored in credentials
        auth.setDetails(Map.of(
            "userId", userId,
            "organizationId", organizationId,
            "tier", tier,
            "tokenType", "JWT"
        ));

        return auth;
    }
}