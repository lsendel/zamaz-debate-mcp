package com.zamaz.mcp.authserver.service;

import com.zamaz.mcp.authserver.entity.User;
import com.zamaz.mcp.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmailAndIsActiveTrue(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .disabled(!user.getIsActive())
            .accountExpired(false)
            .accountLocked(user.getAccountLocked())
            .credentialsExpired(false)
            .authorities(getAuthorities(user))
            .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Get roles from organization memberships
        return user.getOrganizationUsers().stream()
            .map(orgUser -> new SimpleGrantedAuthority("ROLE_" + orgUser.getRole().toUpperCase()))
            .collect(Collectors.toSet());
    }
}