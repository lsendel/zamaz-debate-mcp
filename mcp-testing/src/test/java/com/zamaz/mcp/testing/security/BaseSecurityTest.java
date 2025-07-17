package com.zamaz.mcp.testing.security;

import com.zamaz.mcp.testing.integration.BaseIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Base class for security tests
 */
public abstract class BaseSecurityTest extends BaseIntegrationTest {
    
    protected static final String validToken = generateValidToken();
    private static final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    /**
     * Generate a valid JWT token
     */
    protected static String generateValidToken() {
        return Jwts.builder()
            .setSubject("test-user")
            .claim("organizationId", "test-org")
            .claim("roles", new String[]{"USER"})
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(secretKey)
            .compact();
    }
    
    /**
     * Generate an expired JWT token
     */
    protected String generateExpiredToken() {
        return Jwts.builder()
            .setSubject("test-user")
            .claim("organizationId", "test-org")
            .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
            .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
            .signWith(secretKey)
            .compact();
    }
    
    /**
     * Authenticate with specific role
     */
    protected String authenticateWithRole(String username, String password, String organizationId, String role) {
        // In a real implementation, this would make an actual auth request
        // For testing, we generate a token with the specified role
        return Jwts.builder()
            .setSubject(username)
            .claim("organizationId", organizationId)
            .claim("roles", new String[]{role})
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(secretKey)
            .compact();
    }
}