package com.zamaz.mcp.configserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Security configuration properties with validation.
 * These properties define security settings for all MCP services.
 */
@ConfigurationProperties(prefix = "security")
@Validated
public class SecurityProperties {

    @Valid
    @NestedConfigurationProperty
    private JwtProperties jwt = new JwtProperties();

    @Valid
    @NestedConfigurationProperty
    private CorsProperties cors = new CorsProperties();

    @Valid
    @NestedConfigurationProperty
    private HeadersProperties headers = new HeadersProperties();

    @Valid
    @NestedConfigurationProperty
    private PasswordPolicyProperties passwordPolicy = new PasswordPolicyProperties();

    @Valid
    @NestedConfigurationProperty
    private LockoutPolicyProperties lockoutPolicy = new LockoutPolicyProperties();

    @Valid
    @NestedConfigurationProperty
    private MfaProperties mfa = new MfaProperties();

    @Valid
    @NestedConfigurationProperty
    private AuditProperties audit = new AuditProperties();

    @Valid
    @NestedConfigurationProperty
    private SessionProperties session = new SessionProperties();

    @Valid
    @NestedConfigurationProperty
    private RateLimitingProperties rateLimiting = new RateLimitingProperties();

    @Valid
    @NestedConfigurationProperty
    private EncryptionProperties encryption = new EncryptionProperties();

    /**
     * JWT configuration properties
     */
    public static class JwtProperties {
        @NotBlank(message = "JWT issuer URI is required")
        @Pattern(regexp = "^https?://.+", message = "JWT issuer URI must be a valid URL")
        private String issuerUri;

        @NotBlank(message = "JWT audience is required")
        private String audience;

        @NotBlank(message = "JWT algorithm is required")
        @Pattern(regexp = "^(RS256|RS384|RS512|ES256|ES384|ES512)$", 
                message = "JWT algorithm must be asymmetric (RS256, RS384, RS512, ES256, ES384, ES512)")
        private String algorithm = "RS256";

        @Min(value = 300, message = "Access token validity must be at least 5 minutes")
        @Max(value = 3600, message = "Access token validity must not exceed 1 hour")
        private int accessTokenValidity = 900; // 15 minutes

        @Min(value = 3600, message = "Refresh token validity must be at least 1 hour")
        @Max(value = 604800, message = "Refresh token validity must not exceed 7 days")
        private int refreshTokenValidity = 86400; // 24 hours

        @Min(value = 3600, message = "Key rotation interval must be at least 1 hour")
        private int keyRotationInterval = 86400; // 24 hours

        // Getters and setters
        public String getIssuerUri() { return issuerUri; }
        public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public int getAccessTokenValidity() { return accessTokenValidity; }
        public void setAccessTokenValidity(int accessTokenValidity) { this.accessTokenValidity = accessTokenValidity; }
        public int getRefreshTokenValidity() { return refreshTokenValidity; }
        public void setRefreshTokenValidity(int refreshTokenValidity) { this.refreshTokenValidity = refreshTokenValidity; }
        public int getKeyRotationInterval() { return keyRotationInterval; }
        public void setKeyRotationInterval(int keyRotationInterval) { this.keyRotationInterval = keyRotationInterval; }
    }

    /**
     * CORS configuration properties
     */
    public static class CorsProperties {
        @NotNull(message = "CORS allowed origins is required")
        @Size(min = 1, message = "At least one allowed origin must be specified")
        private List<@NotBlank @Pattern(regexp = "^https?://.+") String> allowedOrigins;

        @NotNull(message = "CORS allowed methods is required")
        @Size(min = 1, message = "At least one allowed method must be specified")
        private List<@NotBlank String> allowedMethods;

        @NotNull(message = "CORS allowed headers is required")
        private List<String> allowedHeaders;

        private List<String> exposeHeaders;

        private boolean allowCredentials = true;

        @Min(value = 0, message = "CORS max age must be non-negative")
        @Max(value = 86400, message = "CORS max age must not exceed 24 hours")
        private long maxAge = 3600;

        // Getters and setters
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
        public List<String> getExposeHeaders() { return exposeHeaders; }
        public void setExposeHeaders(List<String> exposeHeaders) { this.exposeHeaders = exposeHeaders; }
        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }

    /**
     * Security headers configuration properties
     */
    public static class HeadersProperties {
        @NotBlank(message = "X-Frame-Options header is required")
        @Pattern(regexp = "^(DENY|SAMEORIGIN)$", message = "X-Frame-Options must be DENY or SAMEORIGIN")
        private String frameOptions = "DENY";

        @NotBlank(message = "X-XSS-Protection header is required")
        private String xssProtection = "1; mode=block";

        @NotBlank(message = "X-Content-Type-Options header is required")
        private String contentTypeOptions = "nosniff";

        @NotBlank(message = "Referrer-Policy header is required")
        private String referrerPolicy = "strict-origin-when-cross-origin";

        @NotBlank(message = "Content-Security-Policy header is required")
        private String contentSecurityPolicy;

        private String strictTransportSecurity;
        private String permissionsPolicy;

        // Getters and setters
        public String getFrameOptions() { return frameOptions; }
        public void setFrameOptions(String frameOptions) { this.frameOptions = frameOptions; }
        public String getXssProtection() { return xssProtection; }
        public void setXssProtection(String xssProtection) { this.xssProtection = xssProtection; }
        public String getContentTypeOptions() { return contentTypeOptions; }
        public void setContentTypeOptions(String contentTypeOptions) { this.contentTypeOptions = contentTypeOptions; }
        public String getReferrerPolicy() { return referrerPolicy; }
        public void setReferrerPolicy(String referrerPolicy) { this.referrerPolicy = referrerPolicy; }
        public String getContentSecurityPolicy() { return contentSecurityPolicy; }
        public void setContentSecurityPolicy(String contentSecurityPolicy) { this.contentSecurityPolicy = contentSecurityPolicy; }
        public String getStrictTransportSecurity() { return strictTransportSecurity; }
        public void setStrictTransportSecurity(String strictTransportSecurity) { this.strictTransportSecurity = strictTransportSecurity; }
        public String getPermissionsPolicy() { return permissionsPolicy; }
        public void setPermissionsPolicy(String permissionsPolicy) { this.permissionsPolicy = permissionsPolicy; }
    }

    /**
     * Password policy configuration properties
     */
    public static class PasswordPolicyProperties {
        @Min(value = 8, message = "Minimum password length must be at least 8 characters")
        @Max(value = 20, message = "Minimum password length must not exceed 20 characters")
        private int minLength = 12;

        @Min(value = 64, message = "Maximum password length must be at least 64 characters")
        @Max(value = 256, message = "Maximum password length must not exceed 256 characters")
        private int maxLength = 128;

        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigits = true;
        private boolean requireSpecial = true;

        @NotBlank(message = "Special characters list is required")
        private String specialCharacters = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        @Min(value = 0, message = "Password history count must be non-negative")
        @Max(value = 24, message = "Password history count must not exceed 24")
        private int passwordHistoryCount = 5;

        @Min(value = 0, message = "Maximum password age must be non-negative")
        @Max(value = 365, message = "Maximum password age must not exceed 365 days")
        private int maxAgeDays = 90;

        @Min(value = 0, message = "Minimum password age must be non-negative")
        @Max(value = 168, message = "Minimum password age must not exceed 7 days")
        private int minAgeHours = 24;

        private boolean breachCheckEnabled = false;

        // Getters and setters
        public int getMinLength() { return minLength; }
        public void setMinLength(int minLength) { this.minLength = minLength; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
        public boolean isRequireUppercase() { return requireUppercase; }
        public void setRequireUppercase(boolean requireUppercase) { this.requireUppercase = requireUppercase; }
        public boolean isRequireLowercase() { return requireLowercase; }
        public void setRequireLowercase(boolean requireLowercase) { this.requireLowercase = requireLowercase; }
        public boolean isRequireDigits() { return requireDigits; }
        public void setRequireDigits(boolean requireDigits) { this.requireDigits = requireDigits; }
        public boolean isRequireSpecial() { return requireSpecial; }
        public void setRequireSpecial(boolean requireSpecial) { this.requireSpecial = requireSpecial; }
        public String getSpecialCharacters() { return specialCharacters; }
        public void setSpecialCharacters(String specialCharacters) { this.specialCharacters = specialCharacters; }
        public int getPasswordHistoryCount() { return passwordHistoryCount; }
        public void setPasswordHistoryCount(int passwordHistoryCount) { this.passwordHistoryCount = passwordHistoryCount; }
        public int getMaxAgeDays() { return maxAgeDays; }
        public void setMaxAgeDays(int maxAgeDays) { this.maxAgeDays = maxAgeDays; }
        public int getMinAgeHours() { return minAgeHours; }
        public void setMinAgeHours(int minAgeHours) { this.minAgeHours = minAgeHours; }
        public boolean isBreachCheckEnabled() { return breachCheckEnabled; }
        public void setBreachCheckEnabled(boolean breachCheckEnabled) { this.breachCheckEnabled = breachCheckEnabled; }
    }

    /**
     * Account lockout policy configuration properties
     */
    public static class LockoutPolicyProperties {
        private boolean enabled = true;

        @Min(value = 3, message = "Maximum attempts must be at least 3")
        @Max(value = 10, message = "Maximum attempts must not exceed 10")
        private int maxAttempts = 5;

        @Min(value = 300, message = "Lockout duration must be at least 5 minutes")
        @Max(value = 86400, message = "Lockout duration must not exceed 24 hours")
        private int lockoutDuration = 900; // 15 minutes

        @Min(value = 600, message = "Reset duration must be at least 10 minutes")
        private int resetDuration = 1800; // 30 minutes

        private boolean exponentialBackoff = true;

        @Min(value = 100, message = "Base delay must be at least 100ms")
        private long baseDelay = 1000; // 1 second

        @Max(value = 600000, message = "Max delay must not exceed 10 minutes")
        private long maxDelay = 300000; // 5 minutes

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public int getLockoutDuration() { return lockoutDuration; }
        public void setLockoutDuration(int lockoutDuration) { this.lockoutDuration = lockoutDuration; }
        public int getResetDuration() { return resetDuration; }
        public void setResetDuration(int resetDuration) { this.resetDuration = resetDuration; }
        public boolean isExponentialBackoff() { return exponentialBackoff; }
        public void setExponentialBackoff(boolean exponentialBackoff) { this.exponentialBackoff = exponentialBackoff; }
        public long getBaseDelay() { return baseDelay; }
        public void setBaseDelay(long baseDelay) { this.baseDelay = baseDelay; }
        public long getMaxDelay() { return maxDelay; }
        public void setMaxDelay(long maxDelay) { this.maxDelay = maxDelay; }
    }

    /**
     * MFA configuration properties
     */
    public static class MfaProperties {
        @Valid
        @NestedConfigurationProperty
        private TotpProperties totp = new TotpProperties();

        @Valid
        @NestedConfigurationProperty
        private BackupCodesProperties backupCodes = new BackupCodesProperties();

        public static class TotpProperties {
            private boolean enabled = true;

            @NotBlank(message = "TOTP issuer is required")
            private String issuer = "MCP Services";

            @Min(value = 6, message = "TOTP digits must be at least 6")
            @Max(value = 8, message = "TOTP digits must not exceed 8")
            private int digits = 6;

            @Min(value = 20, message = "TOTP period must be at least 20 seconds")
            @Max(value = 60, message = "TOTP period must not exceed 60 seconds")
            private int period = 30;

            @NotBlank(message = "TOTP algorithm is required")
            @Pattern(regexp = "^(HmacSHA1|HmacSHA256|HmacSHA512)$")
            private String algorithm = "HmacSHA256";

            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getIssuer() { return issuer; }
            public void setIssuer(String issuer) { this.issuer = issuer; }
            public int getDigits() { return digits; }
            public void setDigits(int digits) { this.digits = digits; }
            public int getPeriod() { return period; }
            public void setPeriod(int period) { this.period = period; }
            public String getAlgorithm() { return algorithm; }
            public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        }

        public static class BackupCodesProperties {
            private boolean enabled = true;

            @Min(value = 5, message = "Backup code count must be at least 5")
            @Max(value = 20, message = "Backup code count must not exceed 20")
            private int count = 10;

            @Min(value = 6, message = "Backup code length must be at least 6")
            @Max(value = 12, message = "Backup code length must not exceed 12")
            private int length = 8;

            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public int getCount() { return count; }
            public void setCount(int count) { this.count = count; }
            public int getLength() { return length; }
            public void setLength(int length) { this.length = length; }
        }

        // Getters and setters
        public TotpProperties getTotp() { return totp; }
        public void setTotp(TotpProperties totp) { this.totp = totp; }
        public BackupCodesProperties getBackupCodes() { return backupCodes; }
        public void setBackupCodes(BackupCodesProperties backupCodes) { this.backupCodes = backupCodes; }
    }

    /**
     * Audit configuration properties
     */
    public static class AuditProperties {
        private boolean enabled = true;

        @Min(value = 7, message = "Retention days must be at least 7")
        @Max(value = 3650, message = "Retention days must not exceed 10 years")
        private int retentionDays = 90;

        @NotNull(message = "High risk events list is required")
        private List<String> highRiskEvents;

        @Min(value = 1, message = "Alert threshold must be at least 1")
        private int alertThreshold = 10;

        @Min(value = 1, message = "Alert window must be at least 1 minute")
        @Max(value = 60, message = "Alert window must not exceed 60 minutes")
        private int alertWindowMinutes = 5;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
        public List<String> getHighRiskEvents() { return highRiskEvents; }
        public void setHighRiskEvents(List<String> highRiskEvents) { this.highRiskEvents = highRiskEvents; }
        public int getAlertThreshold() { return alertThreshold; }
        public void setAlertThreshold(int alertThreshold) { this.alertThreshold = alertThreshold; }
        public int getAlertWindowMinutes() { return alertWindowMinutes; }
        public void setAlertWindowMinutes(int alertWindowMinutes) { this.alertWindowMinutes = alertWindowMinutes; }
    }

    /**
     * Session configuration properties
     */
    public static class SessionProperties {
        @Min(value = 300, message = "Session timeout must be at least 5 minutes")
        @Max(value = 7200, message = "Session timeout must not exceed 2 hours")
        private int timeout = 1800; // 30 minutes

        @Min(value = 1, message = "Max concurrent sessions must be at least 1")
        @Max(value = 10, message = "Max concurrent sessions must not exceed 10")
        private int maxConcurrent = 5;

        @NotBlank(message = "Session store type is required")
        @Pattern(regexp = "^(redis|memory|jdbc)$", message = "Session store must be redis, memory, or jdbc")
        private String storeType = "redis";

        @Valid
        @NestedConfigurationProperty
        private CookieProperties cookie = new CookieProperties();

        public static class CookieProperties {
            @NotBlank(message = "Cookie name is required")
            private String name = "MCP_SESSION";

            private boolean secure = true;
            private boolean httpOnly = true;

            @NotBlank(message = "Same site policy is required")
            @Pattern(regexp = "^(strict|lax|none)$", message = "Same site must be strict, lax, or none")
            private String sameSite = "strict";

            @Min(value = 0, message = "Cookie max age must be non-negative")
            private int maxAge = 1800;

            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public boolean isSecure() { return secure; }
            public void setSecure(boolean secure) { this.secure = secure; }
            public boolean isHttpOnly() { return httpOnly; }
            public void setHttpOnly(boolean httpOnly) { this.httpOnly = httpOnly; }
            public String getSameSite() { return sameSite; }
            public void setSameSite(String sameSite) { this.sameSite = sameSite; }
            public int getMaxAge() { return maxAge; }
            public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
        }

        // Getters and setters
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
        public String getStoreType() { return storeType; }
        public void setStoreType(String storeType) { this.storeType = storeType; }
        public CookieProperties getCookie() { return cookie; }
        public void setCookie(CookieProperties cookie) { this.cookie = cookie; }
    }

    /**
     * Rate limiting configuration properties
     */
    public static class RateLimitingProperties {
        private boolean enabled = true;

        @Min(value = 1, message = "Default limit must be at least 1")
        @Max(value = 10000, message = "Default limit must not exceed 10000")
        private int defaultLimit = 100;

        @Min(value = 1, message = "Default window must be at least 1 second")
        @Max(value = 3600, message = "Default window must not exceed 1 hour")
        private int defaultWindow = 60; // 1 minute

        @Valid
        private List<EndpointLimit> endpoints;

        public static class EndpointLimit {
            @NotBlank(message = "Endpoint path is required")
            private String path;

            @Min(value = 1, message = "Endpoint limit must be at least 1")
            private int limit;

            @Min(value = 1, message = "Endpoint window must be at least 1 second")
            private int window;

            // Getters and setters
            public String getPath() { return path; }
            public void setPath(String path) { this.path = path; }
            public int getLimit() { return limit; }
            public void setLimit(int limit) { this.limit = limit; }
            public int getWindow() { return window; }
            public void setWindow(int window) { this.window = window; }
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDefaultLimit() { return defaultLimit; }
        public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
        public int getDefaultWindow() { return defaultWindow; }
        public void setDefaultWindow(int defaultWindow) { this.defaultWindow = defaultWindow; }
        public List<EndpointLimit> getEndpoints() { return endpoints; }
        public void setEndpoints(List<EndpointLimit> endpoints) { this.endpoints = endpoints; }
    }

    /**
     * Encryption configuration properties
     */
    public static class EncryptionProperties {
        @NotBlank(message = "Encryption algorithm is required")
        @Pattern(regexp = "^AES/(GCM|CBC|CTR)/NoPadding$", message = "Invalid encryption algorithm")
        private String algorithm = "AES/GCM/NoPadding";

        @Min(value = 128, message = "Key size must be at least 128 bits")
        @Max(value = 256, message = "Key size must not exceed 256 bits")
        private int keySize = 256;

        // Getters and setters
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public int getKeySize() { return keySize; }
        public void setKeySize(int keySize) { this.keySize = keySize; }
    }

    // Root-level getters and setters
    public JwtProperties getJwt() { return jwt; }
    public void setJwt(JwtProperties jwt) { this.jwt = jwt; }
    public CorsProperties getCors() { return cors; }
    public void setCors(CorsProperties cors) { this.cors = cors; }
    public HeadersProperties getHeaders() { return headers; }
    public void setHeaders(HeadersProperties headers) { this.headers = headers; }
    public PasswordPolicyProperties getPasswordPolicy() { return passwordPolicy; }
    public void setPasswordPolicy(PasswordPolicyProperties passwordPolicy) { this.passwordPolicy = passwordPolicy; }
    public LockoutPolicyProperties getLockoutPolicy() { return lockoutPolicy; }
    public void setLockoutPolicy(LockoutPolicyProperties lockoutPolicy) { this.lockoutPolicy = lockoutPolicy; }
    public MfaProperties getMfa() { return mfa; }
    public void setMfa(MfaProperties mfa) { this.mfa = mfa; }
    public AuditProperties getAudit() { return audit; }
    public void setAudit(AuditProperties audit) { this.audit = audit; }
    public SessionProperties getSession() { return session; }
    public void setSession(SessionProperties session) { this.session = session; }
    public RateLimitingProperties getRateLimiting() { return rateLimiting; }
    public void setRateLimiting(RateLimitingProperties rateLimiting) { this.rateLimiting = rateLimiting; }
    public EncryptionProperties getEncryption() { return encryption; }
    public void setEncryption(EncryptionProperties encryption) { this.encryption = encryption; }
}