package com.zamaz.mcp.gateway.graphql.security;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Security service for GraphQL operations
 */
@Service
@Slf4j
public class GraphQLSecurityService {

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated(DataFetchingEnvironment environment) {
        Object context = environment.getContext();
        
        if (context instanceof graphql.kickstart.servlet.context.GraphQLServletContext) {
            graphql.kickstart.servlet.context.GraphQLServletContext servletContext = 
                (graphql.kickstart.servlet.context.GraphQLServletContext) context;
            
            String authHeader = servletContext.getHttpServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return validateToken(token);
            }
        }
        
        return false;
    }

    /**
     * Check if user has required role
     */
    public boolean hasRole(DataFetchingEnvironment environment, String requiredRole) {
        String userRole = getUserRole(environment);
        return hasRoleHierarchy(userRole, requiredRole);
    }

    /**
     * Get user role from context
     */
    public String getUserRole(DataFetchingEnvironment environment) {
        Object context = environment.getContext();
        
        if (context instanceof graphql.kickstart.servlet.context.GraphQLServletContext) {
            graphql.kickstart.servlet.context.GraphQLServletContext servletContext = 
                (graphql.kickstart.servlet.context.GraphQLServletContext) context;
            
            String authHeader = servletContext.getHttpServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return extractRoleFromToken(token);
            }
        }
        
        return "ANONYMOUS";
    }

    /**
     * Check if user has specific permission
     */
    public boolean hasPermission(DataFetchingEnvironment environment, String permission) {
        String userRole = getUserRole(environment);
        return hasPermissionForRole(userRole, permission);
    }

    /**
     * Get user ID from context
     */
    public String getUserId(DataFetchingEnvironment environment) {
        Object context = environment.getContext();
        
        if (context instanceof graphql.kickstart.servlet.context.GraphQLServletContext) {
            graphql.kickstart.servlet.context.GraphQLServletContext servletContext = 
                (graphql.kickstart.servlet.context.GraphQLServletContext) context;
            
            String authHeader = servletContext.getHttpServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return extractUserIdFromToken(token);
            }
        }
        
        return null;
    }

    /**
     * Get organization ID from context
     */
    public String getOrganizationId(DataFetchingEnvironment environment) {
        Object context = environment.getContext();
        
        if (context instanceof graphql.kickstart.servlet.context.GraphQLServletContext) {
            graphql.kickstart.servlet.context.GraphQLServletContext servletContext = 
                (graphql.kickstart.servlet.context.GraphQLServletContext) context;
            
            // Try X-Organization-ID header first
            String orgHeader = servletContext.getHttpServletRequest().getHeader("X-Organization-ID");
            if (orgHeader != null) {
                return orgHeader;
            }
            
            // Fall back to token
            String authHeader = servletContext.getHttpServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return extractOrganizationIdFromToken(token);
            }
        }
        
        return null;
    }

    private boolean validateToken(String token) {
        // TODO: Implement actual JWT validation
        // For now, accept any non-empty token
        return token != null && !token.trim().isEmpty();
    }

    private String extractRoleFromToken(String token) {
        // TODO: Implement actual JWT parsing
        // For now, return default role
        return "USER";
    }

    private String extractUserIdFromToken(String token) {
        // TODO: Implement actual JWT parsing
        // For now, return mock user ID
        return "user-123";
    }

    private String extractOrganizationIdFromToken(String token) {
        // TODO: Implement actual JWT parsing
        // For now, return mock organization ID
        return "org-123";
    }

    private boolean hasRoleHierarchy(String userRole, String requiredRole) {
        // Define role hierarchy
        if (userRole == null) {
            return false;
        }
        
        switch (requiredRole) {
            case "USER":
                return userRole.equals("USER") || userRole.equals("ADMIN") || 
                       userRole.equals("MODERATOR") || userRole.equals("SUPER_ADMIN");
            case "MODERATOR":
                return userRole.equals("MODERATOR") || userRole.equals("ADMIN") || 
                       userRole.equals("SUPER_ADMIN");
            case "ADMIN":
                return userRole.equals("ADMIN") || userRole.equals("SUPER_ADMIN");
            case "SUPER_ADMIN":
                return userRole.equals("SUPER_ADMIN");
            default:
                return false;
        }
    }

    private boolean hasPermissionForRole(String userRole, String permission) {
        // TODO: Implement permission checking based on role
        // For now, return true for authenticated users
        return userRole != null && !userRole.equals("ANONYMOUS");
    }
}