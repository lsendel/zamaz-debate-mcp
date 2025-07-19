package com.zamaz.mcp.configserver.validator;

import com.zamaz.mcp.configserver.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.*;

/**
 * Security configuration validator that ensures security settings meet minimum requirements.
 * Performs validation on startup and can be called to validate configurations dynamically.
 */
@Component
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigValidator.class);

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private Validator validator;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${validation.security.enforcement-mode:strict}")
    private String enforcementMode;

    /**
     * Validates security configuration on application startup
     */
    @PostConstruct
    public void validateOnStartup() {
        logger.info("Starting security configuration validation...");
        
        ValidationResult result = validateConfiguration();
        
        if (result.hasErrors()) {
            handleValidationErrors(result);
        } else {
            logger.info("Security configuration validation passed successfully");
        }
    }

    /**
     * Validates the current security configuration
     * @return ValidationResult containing any errors or warnings
     */
    public ValidationResult validateConfiguration() {
        ValidationResult result = new ValidationResult();
        
        // Validate using Bean Validation annotations
        Set<ConstraintViolation<SecurityProperties>> violations = validator.validate(securityProperties);
        for (ConstraintViolation<SecurityProperties> violation : violations) {
            result.addError(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        // Perform custom validation based on environment
        String activeProfile = getActiveProfile();
        validateForEnvironment(activeProfile, result);
        
        // Validate minimum security requirements
        validateMinimumRequirements(result);
        
        // Validate inter-property dependencies
        validatePropertyDependencies(result);
        
        return result;
    }

    /**
     * Validates configuration based on the active environment profile
     */
    private void validateForEnvironment(String profile, ValidationResult result) {
        switch (profile) {
            case "prod":
            case "production":
                validateProductionRequirements(result);
                break;
            case "staging":
                validateStagingRequirements(result);
                break;
            case "dev":
            case "development":
                // Development has relaxed requirements
                logger.debug("Using relaxed validation for development environment");
                break;
            default:
                result.addWarning("environment", "Unknown environment profile: " + profile);
        }
    }

    /**
     * Validates strict requirements for production environment
     */
    private void validateProductionRequirements(ValidationResult result) {
        // JWT Requirements
        if (!"RS256".equals(securityProperties.getJwt().getAlgorithm()) && 
            !"RS384".equals(securityProperties.getJwt().getAlgorithm()) &&
            !"RS512".equals(securityProperties.getJwt().getAlgorithm())) {
            result.addError("jwt.algorithm", "Production must use RSA algorithm (RS256, RS384, or RS512)");
        }
        
        if (securityProperties.getJwt().getAccessTokenValidity() > 600) {
            result.addError("jwt.accessTokenValidity", "Production access tokens must not exceed 10 minutes");
        }
        
        // CORS Requirements
        for (String origin : securityProperties.getCors().getAllowedOrigins()) {
            if (!origin.startsWith("https://") && !origin.equals("https://localhost")) {
                result.addError("cors.allowedOrigins", "Production must only allow HTTPS origins");
            }
        }
        
        // Security Headers
        if (!"DENY".equals(securityProperties.getHeaders().getFrameOptions())) {
            result.addError("headers.frameOptions", "Production must use DENY for X-Frame-Options");
        }
        
        if (securityProperties.getHeaders().getStrictTransportSecurity() == null || 
            !securityProperties.getHeaders().getStrictTransportSecurity().contains("max-age=")) {
            result.addError("headers.strictTransportSecurity", 
                "Production must include Strict-Transport-Security header");
        }
        
        // Password Policy
        if (securityProperties.getPasswordPolicy().getMinLength() < 14) {
            result.addError("passwordPolicy.minLength", 
                "Production passwords must be at least 14 characters");
        }
        
        if (!securityProperties.getPasswordPolicy().isBreachCheckEnabled()) {
            result.addError("passwordPolicy.breachCheckEnabled", 
                "Production must enable password breach checking");
        }
        
        // Account Lockout
        if (!securityProperties.getLockoutPolicy().isEnabled()) {
            result.addError("lockoutPolicy.enabled", 
                "Production must enable account lockout policy");
        }
        
        if (securityProperties.getLockoutPolicy().getMaxAttempts() > 3) {
            result.addError("lockoutPolicy.maxAttempts", 
                "Production must not allow more than 3 failed login attempts");
        }
        
        // MFA
        if (!securityProperties.getMfa().getTotp().isEnabled()) {
            result.addError("mfa.totp.enabled", "Production must enable TOTP MFA");
        }
        
        // Audit
        if (!securityProperties.getAudit().isEnabled()) {
            result.addError("audit.enabled", "Production must enable security auditing");
        }
        
        if (securityProperties.getAudit().getRetentionDays() < 365) {
            result.addError("audit.retentionDays", 
                "Production audit logs must be retained for at least 365 days");
        }
        
        // Session
        if (securityProperties.getSession().getTimeout() > 900) {
            result.addError("session.timeout", 
                "Production session timeout must not exceed 15 minutes");
        }
        
        if (!securityProperties.getSession().getCookie().isSecure()) {
            result.addError("session.cookie.secure", 
                "Production session cookies must be secure");
        }
        
        // Rate Limiting
        if (!securityProperties.getRateLimiting().isEnabled()) {
            result.addError("rateLimiting.enabled", 
                "Production must enable rate limiting");
        }
    }

    /**
     * Validates requirements for staging environment
     */
    private void validateStagingRequirements(ValidationResult result) {
        // Staging should be production-like but with some relaxations
        
        // JWT Requirements
        if (securityProperties.getJwt().getAlgorithm().startsWith("HS")) {
            result.addWarning("jwt.algorithm", 
                "Staging should use asymmetric algorithms like production");
        }
        
        // Password Policy
        if (securityProperties.getPasswordPolicy().getMinLength() < 12) {
            result.addWarning("passwordPolicy.minLength", 
                "Staging passwords should be at least 12 characters");
        }
        
        // MFA
        if (!securityProperties.getMfa().getTotp().isEnabled()) {
            result.addWarning("mfa.totp.enabled", 
                "Staging should enable TOTP MFA like production");
        }
    }

    /**
     * Validates minimum security requirements for all environments
     */
    private void validateMinimumRequirements(ValidationResult result) {
        // Minimum JWT validity
        if (securityProperties.getJwt().getAccessTokenValidity() < 300) {
            result.addError("jwt.accessTokenValidity", 
                "Access token validity must be at least 5 minutes");
        }
        
        // Minimum password length
        if (securityProperties.getPasswordPolicy().getMinLength() < 8) {
            result.addError("passwordPolicy.minLength", 
                "Minimum password length must be at least 8 characters");
        }
        
        // Minimum lockout attempts
        if (securityProperties.getLockoutPolicy().isEnabled() && 
            securityProperties.getLockoutPolicy().getMaxAttempts() < 3) {
            result.addError("lockoutPolicy.maxAttempts", 
                "If lockout is enabled, must allow at least 3 attempts");
        }
        
        // Maximum session timeout
        if (securityProperties.getSession().getTimeout() > 7200) {
            result.addError("session.timeout", 
                "Session timeout must not exceed 2 hours");
        }
    }

    /**
     * Validates dependencies between different properties
     */
    private void validatePropertyDependencies(ValidationResult result) {
        // If MFA is enabled, backup codes should also be enabled
        if (securityProperties.getMfa().getTotp().isEnabled() && 
            !securityProperties.getMfa().getBackupCodes().isEnabled()) {
            result.addWarning("mfa.backupCodes.enabled", 
                "Backup codes should be enabled when TOTP is enabled");
        }
        
        // If breach check is enabled, ensure we have proper configuration
        if (securityProperties.getPasswordPolicy().isBreachCheckEnabled()) {
            String hibpApiKey = environment.getProperty("security.password-policy.breach-check-api");
            if (hibpApiKey == null || hibpApiKey.isEmpty()) {
                result.addError("passwordPolicy.breachCheckApi", 
                    "Breach check API key must be configured when breach check is enabled");
            }
        }
        
        // If session store is Redis, ensure Redis configuration exists
        if ("redis".equals(securityProperties.getSession().getStoreType())) {
            String redisHost = environment.getProperty("spring.redis.host");
            if (redisHost == null || redisHost.isEmpty()) {
                result.addError("session.storeType", 
                    "Redis configuration must be provided when using Redis session store");
            }
        }
        
        // If rate limiting is enabled with Redis, ensure Redis configuration exists
        if (securityProperties.getRateLimiting().isEnabled()) {
            String redisHost = environment.getProperty("spring.redis.host");
            if (redisHost == null || redisHost.isEmpty()) {
                result.addWarning("rateLimiting.enabled", 
                    "Redis configuration should be provided for distributed rate limiting");
            }
        }
    }

    /**
     * Handles validation errors based on enforcement mode
     */
    private void handleValidationErrors(ValidationResult result) {
        String errorMessage = formatValidationErrors(result);
        
        switch (enforcementMode.toLowerCase()) {
            case "strict":
            case "fail-fast":
                logger.error("Security configuration validation failed:\n{}", errorMessage);
                throw new SecurityConfigurationException(
                    "Security configuration validation failed. See logs for details.");
                
            case "warn":
                logger.warn("Security configuration validation warnings:\n{}", errorMessage);
                break;
                
            default:
                logger.info("Security configuration validation issues (enforcement disabled):\n{}", 
                    errorMessage);
        }
    }

    /**
     * Formats validation errors for logging
     */
    private String formatValidationErrors(ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        
        if (!result.getErrors().isEmpty()) {
            sb.append("\nERRORS:\n");
            result.getErrors().forEach((property, messages) -> {
                messages.forEach(message -> 
                    sb.append(String.format("  - %s: %s\n", property, message))
                );
            });
        }
        
        if (!result.getWarnings().isEmpty()) {
            sb.append("\nWARNINGS:\n");
            result.getWarnings().forEach((property, messages) -> {
                messages.forEach(message -> 
                    sb.append(String.format("  - %s: %s\n", property, message))
                );
            });
        }
        
        return sb.toString();
    }

    /**
     * Gets the active Spring profile
     */
    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            return profiles[0];
        }
        return "default";
    }

    /**
     * Validation result holder
     */
    public static class ValidationResult {
        private final Map<String, List<String>> errors = new HashMap<>();
        private final Map<String, List<String>> warnings = new HashMap<>();

        public void addError(String property, String message) {
            errors.computeIfAbsent(property, k -> new ArrayList<>()).add(message);
        }

        public void addWarning(String property, String message) {
            warnings.computeIfAbsent(property, k -> new ArrayList<>()).add(message);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public Map<String, List<String>> getErrors() {
            return Collections.unmodifiableMap(errors);
        }

        public Map<String, List<String>> getWarnings() {
            return Collections.unmodifiableMap(warnings);
        }
    }

    /**
     * Exception thrown when security configuration validation fails
     */
    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
    }
}