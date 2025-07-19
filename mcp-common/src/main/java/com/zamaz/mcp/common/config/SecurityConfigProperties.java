package com.zamaz.mcp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common security configuration properties for JWT and CORS settings.
 * These properties are loaded from the centralized configuration server.
 */
@ConfigurationProperties(prefix = "mcp.security")
@Validated
public class SecurityConfigProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private OAuth2 oauth2 = new OAuth2();
    private Headers headers = new Headers();
    private RateLimit rateLimit = new RateLimit();

    /**
     * JWT configuration properties
     */
    @Validated
    public static class Jwt {
        /**
         * JWT signing secret (should be encrypted in configuration)
         */
        @NotBlank(message = "JWT secret is required")
        private String secret;

        /**
         * JWT token expiration in seconds
         */
        @Min(value = 60, message = "JWT expiration must be at least 60 seconds")
        private long expiration = 86400; // 24 hours

        /**
         * JWT refresh token expiration in seconds
         */
        @Min(value = 300, message = "Refresh token expiration must be at least 300 seconds")
        private long refreshExpiration = 604800; // 7 days

        /**
         * JWT issuer
         */
        @NotBlank(message = "JWT issuer is required")
        private String issuer = "zamaz-mcp";

        /**
         * JWT audience
         */
        private String audience = "mcp-services";

        /**
         * Clock skew tolerance in seconds
         */
        private int clockSkew = 60;

        /**
         * Whether to require expiration
         */
        private boolean requireExpiration = true;

        /**
         * Algorithm to use for JWT signing
         */
        private String algorithm = "HS512";

        // Getters and setters
        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public long getRefreshExpiration() {
            return refreshExpiration;
        }

        public void setRefreshExpiration(long refreshExpiration) {
            this.refreshExpiration = refreshExpiration;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public int getClockSkew() {
            return clockSkew;
        }

        public void setClockSkew(int clockSkew) {
            this.clockSkew = clockSkew;
        }

        public boolean isRequireExpiration() {
            return requireExpiration;
        }

        public void setRequireExpiration(boolean requireExpiration) {
            this.requireExpiration = requireExpiration;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }

    /**
     * CORS configuration properties
     */
    @Validated
    public static class Cors {
        /**
         * Allowed origins for CORS
         */
        private List<String> allowedOrigins = Arrays.asList("http://localhost:3000");

        /**
         * Allowed HTTP methods
         */
        private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");

        /**
         * Allowed headers
         */
        private List<String> allowedHeaders = Arrays.asList("*");

        /**
         * Exposed headers
         */
        private List<String> exposedHeaders = Arrays.asList("X-Total-Count", "X-Page-Number", "X-Page-Size");

        /**
         * Whether to allow credentials
         */
        private boolean allowCredentials = true;

        /**
         * Max age for preflight requests in seconds
         */
        @Min(value = 0, message = "Max age cannot be negative")
        private long maxAge = 3600;

        // Getters and setters
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * OAuth2 configuration properties
     */
    public static class OAuth2 {
        /**
         * OAuth2 issuer URI
         */
        private String issuerUri;

        /**
         * JWK Set URI
         */
        private String jwkSetUri;

        /**
         * Client ID
         */
        private String clientId;

        /**
         * Client secret (should be encrypted)
         */
        private String clientSecret;

        /**
         * Redirect URI
         */
        private String redirectUri;

        /**
         * Scopes
         */
        private List<String> scopes = Arrays.asList("openid", "profile", "email");

        // Getters and setters
        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }
    }

    /**
     * Security headers configuration
     */
    public static class Headers {
        private String contentSecurityPolicy = "default-src 'self'";
        private String xFrameOptions = "DENY";
        private String xContentTypeOptions = "nosniff";
        private String xXssProtection = "1; mode=block";
        private String strictTransportSecurity = "max-age=31536000; includeSubDomains";
        private String referrerPolicy = "strict-origin-when-cross-origin";
        private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

        // Getters and setters
        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public String getxFrameOptions() {
            return xFrameOptions;
        }

        public void setxFrameOptions(String xFrameOptions) {
            this.xFrameOptions = xFrameOptions;
        }

        public String getxContentTypeOptions() {
            return xContentTypeOptions;
        }

        public void setxContentTypeOptions(String xContentTypeOptions) {
            this.xContentTypeOptions = xContentTypeOptions;
        }

        public String getxXssProtection() {
            return xXssProtection;
        }

        public void setxXssProtection(String xXssProtection) {
            this.xXssProtection = xXssProtection;
        }

        public String getStrictTransportSecurity() {
            return strictTransportSecurity;
        }

        public void setStrictTransportSecurity(String strictTransportSecurity) {
            this.strictTransportSecurity = strictTransportSecurity;
        }

        public String getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(String referrerPolicy) {
            this.referrerPolicy = referrerPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }
    }

    /**
     * Rate limiting configuration
     */
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 60;
        private int requestsPerHour = 1000;
        private int requestsPerDay = 10000;
        private String strategy = "TOKEN_BUCKET";

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getRequestsPerHour() {
            return requestsPerHour;
        }

        public void setRequestsPerHour(int requestsPerHour) {
            this.requestsPerHour = requestsPerHour;
        }

        public int getRequestsPerDay() {
            return requestsPerDay;
        }

        public void setRequestsPerDay(int requestsPerDay) {
            this.requestsPerDay = requestsPerDay;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }

    // Getters and setters for main properties
    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2 oauth2) {
        this.oauth2 = oauth2;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
}