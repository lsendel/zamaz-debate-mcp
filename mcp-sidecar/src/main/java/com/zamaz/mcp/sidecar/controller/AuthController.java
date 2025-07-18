package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.common.patterns.BaseController;
import com.zamaz.mcp.sidecar.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Authentication Controller for MCP Sidecar
 * 
 * Provides REST endpoints for user authentication, token management,
 * and session handling.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController extends BaseController {

    private final AuthenticationService authenticationService;

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthenticationService.AuthenticationResult>> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("Login request received for user: {}", request.getUsername());
        
        return authenticationService.authenticate(request.getUsername(), request.getPassword())
                .map(result -> {
                    log.info("Login successful for user: {}", request.getUsername());
                    return ResponseEntity.ok(result);
                })
                .onErrorResume(error -> {
                    log.error("Login failed for user: {}", request.getUsername(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * Token refresh endpoint
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthenticationService.AuthenticationResult>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        log.info("Token refresh request received");
        
        return authenticationService.refreshToken(request.getRefreshToken())
                .map(result -> {
                    log.info("Token refresh successful");
                    return ResponseEntity.ok(result);
                })
                .onErrorResume(error -> {
                    log.error("Token refresh failed", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * User logout endpoint
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<LogoutResponse>> logout(
            @RequestHeader("Authorization") String authHeader) {
        
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .cast(Authentication.class)
                .flatMap(auth -> {
                    String username = auth.getName();
                    String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
                    
                    log.info("Logout request received for user: {}", username);
                    
                    return authenticationService.logout(username, token)
                            .then(Mono.just(ResponseEntity.ok(new LogoutResponse("Logged out successfully"))))
                            .onErrorResume(error -> {
                                log.error("Logout failed for user: {}", username, error);
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new LogoutResponse("Logout failed")));
                            });
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LogoutResponse("Authentication required")));
    }

    /**
     * Get current user information
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<UserInfoResponse>> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .cast(Authentication.class)
                .map(auth -> {
                    UserInfoResponse userInfo = new UserInfoResponse(
                            auth.getName(),
                            auth.getAuthorities().stream()
                                    .map(authority -> authority.getAuthority())
                                    .toList()
                    );
                    return ResponseEntity.ok(userInfo);
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponse>> health() {
        return Mono.just(ResponseEntity.ok(new HealthResponse("Authentication service is healthy")));
    }

    /**
     * Login request DTO
     */
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Refresh token request DTO
     */
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;

        // Getters and setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    /**
     * Logout response DTO
     */
    public static class LogoutResponse {
        private String message;

        public LogoutResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * User info response DTO
     */
    public static class UserInfoResponse {
        private String username;
        private java.util.List<String> authorities;

        public UserInfoResponse(String username, java.util.List<String> authorities) {
            this.username = username;
            this.authorities = authorities;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public java.util.List<String> getAuthorities() { return authorities; }
        public void setAuthorities(java.util.List<String> authorities) { this.authorities = authorities; }
    }

    /**
     * Health response DTO
     */
    public static class HealthResponse {
        private String status;

        public HealthResponse(String status) {
            this.status = status;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}