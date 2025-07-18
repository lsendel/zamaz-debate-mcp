package com.zamaz.mcp.common.security;

/**
 * Thread-local security context holder for MCP common module.
 * This provides a way to store and retrieve security context without depending on mcp-security.
 */
public class SecurityContextHolder {
    
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Get the current security context
     */
    public static SecurityContext getCurrentContext() {
        return contextHolder.get();
    }
    
    /**
     * Set the current security context
     */
    public static void setCurrentContext(SecurityContext context) {
        contextHolder.set(context);
    }
    
    /**
     * Clear the current security context
     */
    public static void clearContext() {
        contextHolder.remove();
    }
    
    /**
     * Check if there is a current security context
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
}