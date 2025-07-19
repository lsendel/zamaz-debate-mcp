package com.zamaz.mcp.configserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Config Server.
 * 
 * Provides basic authentication for accessing configuration endpoints
 * while allowing health checks to be accessed without authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Allow health check endpoint without authentication
                .requestMatchers("/actuator/health").permitAll()
                // Require authentication for all other endpoints
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {})
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }
}