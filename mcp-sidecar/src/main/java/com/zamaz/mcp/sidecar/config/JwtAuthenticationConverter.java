package com.zamaz.mcp.sidecar.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Converter for MCP Sidecar
 * 
 * Converts JWT tokens into Spring Security Authentication objects
 * with proper role and authority mapping.
 */
@Slf4j
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;

    public JwtAuthenticationConverter() {
        this.jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    }

    @Override
    public Mono<org.springframework.security.core.Authentication> convert(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String token = extractToken(exchange);
            if (token == null) {
                return null;
            }

            // This is a simplified implementation
            // In a real scenario, you would decode and validate the JWT here
            return createAuthentication(token);
        });
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private AbstractAuthenticationToken createAuthentication(String token) {
        // This is a simplified implementation
        // In production, you would properly decode and validate the JWT
        try {
            // Create a mock JWT for now
            // In production, use proper JWT decoding
            Jwt jwt = createMockJwt(token);
            Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
            
            return new JwtAuthenticationToken(jwt, authorities);
        } catch (Exception e) {
            log.error("Failed to convert JWT token", e);
            return null;
        }
    }

    private Jwt createMockJwt(String token) {
        // This is a temporary implementation for development
        // In production, replace with proper JWT decoding
        return Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .header("typ", "JWT")
                .claim("sub", "demo")
                .claim("roles", List.of("USER"))
                .claim("iat", System.currentTimeMillis() / 1000)
                .claim("exp", (System.currentTimeMillis() / 1000) + 3600)
                .build();
    }

    /**
     * Converts JWT claims to Spring Security authorities
     */
    private static class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<String> roles = jwt.getClaimAsStringList("roles");
            
            if (roles == null || roles.isEmpty()) {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }
}