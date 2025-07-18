package com.zamaz.mcp.sidecar.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication Service for MCP Sidecar
 * 
 * Handles user authentication, JWT token generation, and session management.
 * Integrates with Redis for session storage and token blacklisting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret:mcp-sidecar-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${jwt.issuer:zamaz-mcp-sidecar}")
    private String jwtIssuer;

    @Value("${jwt.expiration:86400}") // 24 hours in seconds
    private long jwtExpirationSeconds;

    @Value("${jwt.refresh-expiration:604800}") // 7 days in seconds
    private long refreshTokenExpirationSeconds;

    /**
     * Authenticate user and generate JWT tokens
     */
    public Mono<AuthenticationResult> authenticate(String username, String password) {
        log.info("Attempting to authenticate user: {}", username);
        
        return userDetailsService.findByUsername(username)
                .cast(UserDetails.class)
                .flatMap(userDetails -> {
                    if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    }
                    
                    return generateTokens(userDetails)
                            .flatMap(tokens -> storeSession(userDetails.getUsername(), tokens)
                                    .then(Mono.just(AuthenticationResult.builder()
                                            .accessToken(tokens.accessToken())
                                            .refreshToken(tokens.refreshToken())
                                            .expiresIn(jwtExpirationSeconds)
                                            .tokenType("Bearer")
                                            .username(userDetails.getUsername())
                                            .authorities(userDetails.getAuthorities().stream()
                                                    .map(auth -> auth.getAuthority())
                                                    .toList())
                                            .build())));
                })
                .doOnSuccess(result -> log.info("User authenticated successfully: {}", username))
                .doOnError(error -> log.error("Authentication failed for user: {}", username, error));
    }

    /**
     * Refresh JWT token using refresh token
     */
    public Mono<AuthenticationResult> refreshToken(String refreshToken) {
        log.info("Attempting to refresh token");
        
        return validateRefreshToken(refreshToken)
                .flatMap(username -> userDetailsService.findByUsername(username)
                        .cast(UserDetails.class)
                        .flatMap(userDetails -> generateTokens(userDetails)
                                .flatMap(tokens -> storeSession(userDetails.getUsername(), tokens)
                                        .then(Mono.just(AuthenticationResult.builder()
                                                .accessToken(tokens.accessToken())
                                                .refreshToken(tokens.refreshToken())
                                                .expiresIn(jwtExpirationSeconds)
                                                .tokenType("Bearer")
                                                .username(userDetails.getUsername())
                                                .authorities(userDetails.getAuthorities().stream()
                                                        .map(auth -> auth.getAuthority())
                                                        .toList())
                                                .build()))));
    }

    /**
     * Logout user and invalidate tokens
     */
    public Mono<Void> logout(String username, String accessToken) {
        log.info("Logging out user: {}", username);
        
        return blacklistToken(accessToken)
                .then(redisTemplate.delete("session:" + username))
                .then()
                .doOnSuccess(v -> log.info("User logged out successfully: {}", username));
    }

    /**
     * Validate JWT token
     */
    public Mono<Claims> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                
                return Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception e) {
                log.error("Token validation failed", e);
                throw new BadCredentialsException("Invalid token");
            }
        })
        .flatMap(claims -> isTokenBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return Mono.error(new BadCredentialsException("Token has been revoked"));
                    }
                    return Mono.just(claims);
                }));
    }

    /**
     * Generate access and refresh tokens
     */
    private Mono<TokenPair> generateTokens(UserDetails userDetails) {
        return Mono.fromCallable(() -> {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList();

            // Generate access token
            String accessToken = Jwts.builder()
                    .setSubject(userDetails.getUsername())
                    .setIssuer(jwtIssuer)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(jwtExpirationSeconds)))
                    .claim("roles", roles)
                    .claim("type", "access")
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            // Generate refresh token
            String refreshToken = Jwts.builder()
                    .setSubject(userDetails.getUsername())
                    .setIssuer(jwtIssuer)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(refreshTokenExpirationSeconds)))
                    .claim("type", "refresh")
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            return new TokenPair(accessToken, refreshToken);
        });
    }

    /**
     * Store user session in Redis
     */
    private Mono<Void> storeSession(String username, TokenPair tokens) {
        String sessionKey = "session:" + username;
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("accessToken", tokens.accessToken());
        sessionData.put("refreshToken", tokens.refreshToken());
        sessionData.put("createdAt", Instant.now().toString());
        
        return redisTemplate.opsForHash()
                .putAll(sessionKey, sessionData)
                .then(redisTemplate.expire(sessionKey, Duration.ofSeconds(refreshTokenExpirationSeconds)))
                .then();
    }

    /**
     * Validate refresh token
     */
    private Mono<String> validateRefreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(refreshToken)
                        .getBody();
                
                if (!"refresh".equals(claims.get("type"))) {
                    throw new BadCredentialsException("Invalid token type");
                }
                
                return claims.getSubject();
            } catch (Exception e) {
                log.error("Refresh token validation failed", e);
                throw new BadCredentialsException("Invalid refresh token");
            }
        });
    }

    /**
     * Blacklist token
     */
    private Mono<Void> blacklistToken(String token) {
        String blacklistKey = "blacklist:" + token;
        return redisTemplate.opsForValue()
                .set(blacklistKey, "true", Duration.ofSeconds(jwtExpirationSeconds))
                .then();
    }

    /**
     * Check if token is blacklisted
     */
    private Mono<Boolean> isTokenBlacklisted(String token) {
        String blacklistKey = "blacklist:" + token;
        return redisTemplate.hasKey(blacklistKey);
    }

    /**
     * Token pair record
     */
    private record TokenPair(String accessToken, String refreshToken) {}

    /**
     * Authentication result
     */
    public static class AuthenticationResult {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;
        private String tokenType;
        private String username;
        private List<String> authorities;

        public static AuthenticationResultBuilder builder() {
            return new AuthenticationResultBuilder();
        }

        // Getters and setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        
        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
        
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public List<String> getAuthorities() { return authorities; }
        public void setAuthorities(List<String> authorities) { this.authorities = authorities; }

        public static class AuthenticationResultBuilder {
            private String accessToken;
            private String refreshToken;
            private long expiresIn;
            private String tokenType;
            private String username;
            private List<String> authorities;

            public AuthenticationResultBuilder accessToken(String accessToken) {
                this.accessToken = accessToken;
                return this;
            }

            public AuthenticationResultBuilder refreshToken(String refreshToken) {
                this.refreshToken = refreshToken;
                return this;
            }

            public AuthenticationResultBuilder expiresIn(long expiresIn) {
                this.expiresIn = expiresIn;
                return this;
            }

            public AuthenticationResultBuilder tokenType(String tokenType) {
                this.tokenType = tokenType;
                return this;
            }

            public AuthenticationResultBuilder username(String username) {
                this.username = username;
                return this;
            }

            public AuthenticationResultBuilder authorities(List<String> authorities) {
                this.authorities = authorities;
                return this;
            }

            public AuthenticationResult build() {
                AuthenticationResult result = new AuthenticationResult();
                result.accessToken = this.accessToken;
                result.refreshToken = this.refreshToken;
                result.expiresIn = this.expiresIn;
                result.tokenType = this.tokenType;
                result.username = this.username;
                result.authorities = this.authorities;
                return result;
            }
        }
    }
}