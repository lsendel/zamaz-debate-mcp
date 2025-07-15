package com.zamaz.mcp.security.service;

import com.zamaz.mcp.security.exception.TokenValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 * Provides consistent JWT handling across all MCP services.
 */
@Slf4j
@Service
public class JwtService {
    
    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private final String issuer;
    
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity:3600}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity:86400}") long refreshTokenValidity,
            @Value("${jwt.issuer:mcp-services}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity * 1000; // Convert to milliseconds
        this.refreshTokenValidity = refreshTokenValidity * 1000;
        this.issuer = issuer;
    }
    
    /**
     * Generate an access token
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, accessTokenValidity);
    }
    
    /**
     * Generate a refresh token
     */
    public String generateRefreshToken(String subject) {
        return generateToken(subject, Map.of("type", "refresh"), refreshTokenValidity);
    }
    
    /**
     * Generate a token with custom claims
     */
    private String generateToken(String subject, Map<String, Object> claims, long validity) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);
        
        JwtBuilder builder = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setSubject(subject)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiration);
            
        if (claims != null && !claims.isEmpty()) {
            builder.addClaims(claims);
        }
        
        return builder.signWith(key).compact();
    }
    
    /**
     * Validate and parse a token
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            throw new TokenValidationException("Token has expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new TokenValidationException("Token is not supported", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new TokenValidationException("Token is malformed", e);
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new TokenValidationException("Token signature is invalid", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new TokenValidationException("Token is invalid", e);
        }
    }
    
    /**
     * Extract subject from token
     */
    public String getSubject(String token) {
        return validateToken(token).getSubject();
    }
    
    /**
     * Extract a specific claim from token
     */
    public Object getClaim(String token, String claimName) {
        return validateToken(token).get(claimName);
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (TokenValidationException e) {
            return true;
        }
    }
    
    /**
     * Custom exception for token validation errors
     */
    public static class TokenValidationException extends RuntimeException {
        public TokenValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}