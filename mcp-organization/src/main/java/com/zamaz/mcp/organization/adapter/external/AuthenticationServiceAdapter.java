package com.zamaz.mcp.organization.adapter.external;

import com.zamaz.mcp.common.architecture.adapter.external.ExternalServiceAdapter;
import com.zamaz.mcp.common.architecture.exception.ExternalServiceException;
import com.zamaz.mcp.organization.application.port.outbound.AuthenticationService;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.Role;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of AuthenticationService using JWT tokens.
 * This is an external service adapter in hexagonal architecture.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationServiceAdapter implements AuthenticationService, ExternalServiceAdapter {
    
    private final OrganizationRepository organizationRepository;
    
    @Value("${jwt.secret:default-secret-key-for-development-only-change-in-production}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}") // 24 hours
    private long jwtExpiration;
    
    @Value("${jwt.email-verification-expiration:3600000}") // 1 hour
    private long emailVerificationExpiration;
    
    @Override
    public boolean hasPermission(String userId, String organizationId, Role requiredRole) {
        try {
            var organization = organizationRepository.findById(OrganizationId.from(organizationId))
                .orElse(null);
            
            if (organization == null) {
                return false;
            }
            
            return organization.hasRole(UserId.from(userId), requiredRole);
            
        } catch (Exception e) {
            throw new ExternalServiceException("AuthenticationService", "Failed to check permissions", e);
        }
    }
    
    @Override
    public String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName(); // Assuming username is the user ID
            }
            return null;
        } catch (Exception e) {
            throw new ExternalServiceException("AuthenticationService", "Failed to get current user", e);
        }
    }
    
    @Override
    public String validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return claims.getSubject();
            
        } catch (Exception e) {
            // Invalid token
            return null;
        }
    }
    
    @Override
    public String generateToken(User user, Organization organization) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail().value());
            claims.put("firstName", user.getFirstName().value());
            claims.put("lastName", user.getLastName().value());
            
            if (organization != null) {
                claims.put("organizationId", organization.getId().value());
                claims.put("organizationName", organization.getName().value());
                var userRole = organization.getUserRole(user.getId());
                userRole.ifPresent(role -> claims.put("role", role.name()));
            }
            
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration);
            
            return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().value().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
                
        } catch (Exception e) {
            throw new ExternalServiceException("AuthenticationService", "Failed to generate token", e);
        }
    }
    
    @Override
    public String generateEmailVerificationToken(User user) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("type", "email-verification");
            claims.put("email", user.getEmail().value());
            
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + emailVerificationExpiration);
            
            return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().value().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
                
        } catch (Exception e) {
            throw new ExternalServiceException("AuthenticationService", "Failed to generate verification token", e);
        }
    }
    
    @Override
    public String validateEmailVerificationToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String type = (String) claims.get("type");
            if (!"email-verification".equals(type)) {
                return null;
            }
            
            return claims.getSubject();
            
        } catch (Exception e) {
            // Invalid token
            return null;
        }
    }
    
    /**
     * Gets the signing key for JWT operations.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}