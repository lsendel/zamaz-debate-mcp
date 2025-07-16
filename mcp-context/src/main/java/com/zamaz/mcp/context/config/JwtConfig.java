package com.zamaz.mcp.context.config;

import com.zamaz.mcp.security.filter.JwtAuthenticationFilter;
import com.zamaz.mcp.security.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * JWT configuration for the Context service.
 */
@Configuration
public class JwtConfig {
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        // Simple implementation - in production, this would connect to user service
        return username -> null;
    }
}