package com.zamaz.mcp.organization.service;

import com.zamaz.mcp.common.security.UserLookupService;
import com.zamaz.mcp.organization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of UserLookupService that uses the organization module's UserRepository.
 * This implementation breaks the circular dependency between mcp-security and mcp-organization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserLookupServiceImpl implements UserLookupService {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserDetails> findUserDetailsByEmail(String email) {
        log.debug("Looking up user by email: {}", email);
        
        return userRepository.findByEmailAndIsActiveTrue(email)
                .map(user -> {
                    log.debug("Found active user: {}", user.getEmail());
                    // Convert to McpUser which implements UserDetails
                    return createUserDetails(user);
                });
    }
    
    /**
     * Create UserDetails from User entity.
     * This method creates a simple UserDetails implementation instead of 
     * depending on McpUser to avoid circular dependencies.
     */
    private UserDetails createUserDetails(com.zamaz.mcp.organization.entity.User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER") // Basic role, can be enhanced later
                .accountExpired(false)
                .accountLocked(!user.getIsActive())
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }
}