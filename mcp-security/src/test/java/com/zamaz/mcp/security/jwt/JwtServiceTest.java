package com.zamaz.mcp.security.jwt;

import com.zamaz.mcp.security.model.McpUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey secretKey;
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000; // 24 hours
    private static final String USER_ID = "user123";
    private static final String USERNAME = "testuser";
    private static final String ORG_ID = "org123";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
        
        // Initialize the service (this would normally be done by Spring)
        ReflectionTestUtils.invokeMethod(jwtService, "init");
        
        // Create the same key for verification
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateToken_WithValidUser_ShouldReturnValidToken() {
        // Given
        McpUser user = createTestUser();

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify token structure (JWT has 3 parts separated by dots)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void generateToken_ShouldIncludeAllClaims() {
        // Given
        McpUser user = createTestUser();
        user.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));

        // When
        String token = jwtService.generateToken(user);

        // Then
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(USER_ID, claims.getSubject());
        assertEquals(USERNAME, claims.get("username"));
        assertEquals(ORG_ID, claims.get("organizationId"));
        assertEquals(Arrays.asList("org123", "org456"), claims.get("organizationIds"));
        assertEquals(Arrays.asList("ROLE_USER", "ROLE_ADMIN"), claims.get("roles"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void extractUserId_WithValidToken_ShouldReturnUserId() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertEquals(USER_ID, extractedUserId);
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertEquals(USERNAME, extractedUsername);
    }

    @Test
    void extractOrganizationId_WithValidToken_ShouldReturnOrgId() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When
        String extractedOrgId = jwtService.extractOrganizationId(token);

        // Then
        assertEquals(ORG_ID, extractedOrgId);
    }

    @Test
    void extractOrganizationIds_WithValidToken_ShouldReturnOrgIds() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When
        List<String> extractedOrgIds = jwtService.extractOrganizationIds(token);

        // Then
        assertEquals(Arrays.asList("org123", "org456"), extractedOrgIds);
    }

    @Test
    void extractRoles_WithValidToken_ShouldReturnRoles() {
        // Given
        McpUser user = createTestUser();
        user.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
        String token = jwtService.generateToken(user);

        // When
        List<String> extractedRoles = jwtService.extractRoles(token);

        // Then
        assertEquals(Arrays.asList("ROLE_USER", "ROLE_ADMIN"), extractedRoles);
    }

    @Test
    void isTokenValid_WithValidToken_ShouldReturnTrue() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        // Given - Create an expired token
        Date past = new Date(System.currentTimeMillis() - 1000); // 1 second ago
        String expiredToken = Jwts.builder()
                .setSubject(USER_ID)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(past)
                .signWith(secretKey)
                .compact();

        // When
        boolean isValid = jwtService.isTokenValid(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithInvalidSignature_ShouldReturnFalse() {
        // Given - Create token with different key
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String invalidToken = Jwts.builder()
                .setSubject(USER_ID)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(wrongKey)
                .compact();

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When
        boolean isValid = jwtService.isTokenValid(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithNullToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtService.isTokenValid(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithEmptyToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtService.isTokenValid("");

        // Then
        assertFalse(isValid);
    }

    @Test
    void extractExpiration_WithValidToken_ShouldReturnCorrectDate() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
        
        // Verify expiration is approximately 24 hours from now
        long diff = expiration.getTime() - System.currentTimeMillis();
        assertTrue(diff > EXPIRATION - 5000); // Within 5 seconds
        assertTrue(diff <= EXPIRATION);
    }

    @Test
    void extractClaim_WithCustomExtractor_ShouldWork() {
        // Given
        McpUser user = createTestUser();
        String token = jwtService.generateToken(user);

        // When - Extract issued at time
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        // Then
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()));
    }

    @Test
    void generateToken_WithNullRoles_ShouldNotIncludeRolesClaim() {
        // Given
        McpUser user = createTestUser();
        user.setRoles(null);

        // When
        String token = jwtService.generateToken(user);

        // Then
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertNull(claims.get("roles"));
    }

    @Test
    void extractUserId_WithExpiredToken_ShouldThrowException() {
        // Given - Create an expired token
        Date past = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = Jwts.builder()
                .setSubject(USER_ID)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(past)
                .signWith(secretKey)
                .compact();

        // When & Then
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUserId(expiredToken));
    }

    @Test
    void refreshToken_ShouldGenerateNewTokenWithSameClaims() {
        // Given
        McpUser user = createTestUser();
        user.setRoles(Arrays.asList("ROLE_USER"));
        String originalToken = jwtService.generateToken(user);

        // Wait a bit to ensure different issued time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        // When
        String refreshedToken = jwtService.refreshToken(originalToken);

        // Then
        assertNotNull(refreshedToken);
        assertNotEquals(originalToken, refreshedToken);
        
        // Verify claims are preserved
        assertEquals(jwtService.extractUserId(originalToken), jwtService.extractUserId(refreshedToken));
        assertEquals(jwtService.extractUsername(originalToken), jwtService.extractUsername(refreshedToken));
        assertEquals(jwtService.extractOrganizationId(originalToken), jwtService.extractOrganizationId(refreshedToken));
        assertEquals(jwtService.extractRoles(originalToken), jwtService.extractRoles(refreshedToken));
        
        // Verify new token has later issue time
        Claims originalClaims = jwtService.extractAllClaims(originalToken);
        Claims refreshedClaims = jwtService.extractAllClaims(refreshedToken);
        assertTrue(refreshedClaims.getIssuedAt().after(originalClaims.getIssuedAt()));
    }

    private McpUser createTestUser() {
        McpUser user = new McpUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setEmail("test@example.com");
        user.setCurrentOrganizationId(ORG_ID);
        user.setOrganizationIds(Arrays.asList("org123", "org456"));
        return user;
    }
}