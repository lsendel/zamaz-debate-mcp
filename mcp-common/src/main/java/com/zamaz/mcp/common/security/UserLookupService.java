package com.zamaz.mcp.common.security;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Interface for user lookup operations used by security components.
 * This interface breaks the circular dependency between mcp-security and mcp-organization.
 */
public interface UserLookupService {
    
    /**
     * Find a user by email address and return as UserDetails for Spring Security.
     * 
     * @param email The email address to search for
     * @return Optional containing UserDetails if user found, empty otherwise
     */
    Optional<UserDetails> findUserDetailsByEmail(String email);
}