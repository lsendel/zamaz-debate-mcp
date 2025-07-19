package com.zamaz.mcp.configserver.validator;

import com.zamaz.mcp.configserver.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityConfigValidator
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigValidatorTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private Validator validator;

    @Mock
    private Environment environment;

    @InjectMocks
    private SecurityConfigValidator configValidator;

    private SecurityProperties.JwtProperties jwtProperties;
    private SecurityProperties.CorsProperties corsProperties;
    private SecurityProperties.PasswordPolicyProperties passwordProperties;
    private SecurityProperties.LockoutPolicyProperties lockoutProperties;

    @BeforeEach
    void setUp() {
        // Initialize mock properties
        jwtProperties = new SecurityProperties.JwtProperties();
        jwtProperties.setIssuerUri("https://auth.test.com");
        jwtProperties.setAudience("test-audience");
        jwtProperties.setAlgorithm("RS256");
        jwtProperties.setAccessTokenValidity(900);

        corsProperties = new SecurityProperties.CorsProperties();
        corsProperties.setAllowedOrigins(Arrays.asList("https://app.test.com"));
        corsProperties.setAllowedMethods(Arrays.asList("GET", "POST"));

        passwordProperties = new SecurityProperties.PasswordPolicyProperties();
        passwordProperties.setMinLength(12);
        passwordProperties.setBreachCheckEnabled(false);

        lockoutProperties = new SecurityProperties.LockoutPolicyProperties();
        lockoutProperties.setEnabled(true);
        lockoutProperties.setMaxAttempts(5);

        when(securityProperties.getJwt()).thenReturn(jwtProperties);
        when(securityProperties.getCors()).thenReturn(corsProperties);
        when(securityProperties.getPasswordPolicy()).thenReturn(passwordProperties);
        when(securityProperties.getLockoutPolicy()).thenReturn(lockoutProperties);
    }

    @Test
    void validateConfiguration_withValidConfig_returnsNoErrors() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // When
        SecurityConfigValidator.ValidationResult result = configValidator.validateConfiguration();

        // Then
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    void validateConfiguration_withConstraintViolations_returnsErrors() {
        // Given
        ConstraintViolation<SecurityProperties> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(() -> "jwt.issuerUri");
        when(violation.getMessage()).thenReturn("JWT issuer URI is required");
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        violations.add(violation);
        
        when(validator.validate(any())).thenReturn(violations);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // When
        SecurityConfigValidator.ValidationResult result = configValidator.validateConfiguration();

        // Then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).containsKey("jwt.issuerUri");
        assertThat(result.getErrors().get("jwt.issuerUri"))
            .contains("JWT issuer URI is required");
    }

    @Test
    void validateConfiguration_inProduction_withInsecureSettings_returnsErrors() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        
        // Set insecure values for production
        jwtProperties.setAlgorithm("HS256"); // Symmetric algorithm
        jwtProperties.setAccessTokenValidity(3600); // 1 hour (too long)
        corsProperties.setAllowedOrigins(Arrays.asList("http://app.test.com")); // HTTP
        passwordProperties.setMinLength(8); // Too short
        passwordProperties.setBreachCheckEnabled(false);
        lockoutProperties.setEnabled(false);

        // When
        SecurityConfigValidator.ValidationResult result = configValidator.validateConfiguration();

        // Then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).containsKeys(
            "jwt.algorithm",
            "jwt.accessTokenValidity",
            "cors.allowedOrigins",
            "passwordPolicy.minLength",
            "passwordPolicy.breachCheckEnabled",
            "lockoutPolicy.enabled"
        );
    }

    @Test
    void validateConfiguration_inStaging_withSuboptimalSettings_returnsWarnings() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        
        // Set suboptimal values for staging
        jwtProperties.setAlgorithm("HS256");
        passwordProperties.setMinLength(10);

        // When
        SecurityConfigValidator.ValidationResult result = configValidator.validateConfiguration();

        // Then
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).containsKeys(
            "jwt.algorithm",
            "passwordPolicy.minLength"
        );
    }

    @Test
    void validateConfiguration_withMinimumRequirementsViolation_returnsErrors() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        
        // Violate minimum requirements
        jwtProperties.setAccessTokenValidity(240); // Less than 5 minutes
        passwordProperties.setMinLength(6); // Less than 8
        lockoutProperties.setMaxAttempts(2); // Less than 3

        // Create session properties
        SecurityProperties.SessionProperties sessionProperties = 
            new SecurityProperties.SessionProperties();
        sessionProperties.setTimeout(10800); // More than 2 hours
        when(securityProperties.getSession()).thenReturn(sessionProperties);

        // When
        SecurityConfigValidator.ValidationResult result = configValidator.validateConfiguration();

        // Then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).containsKeys(
            "jwt.accessTokenValidity",
            "passwordPolicy.minLength",
            "lockoutPolicy.maxAttempts",
            "session.timeout"
        );
    }

    @Test
    void validateConfiguration_withPropertyDependencies_detectsIssues() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        
        // Enable MFA without backup codes
        SecurityProperties.MfaProperties mfaProperties = new SecurityProperties.MfaProperties();
        SecurityProperties.MfaProperties.TotpProperties totpProperties = 
            new SecurityProperties.MfaProperties.TotpProperties();
        totpProperties.setEnabled(true);
        SecurityProperties.MfaProperties.BackupCodesProperties backupCodesProperties = 
            new SecurityProperties.MfaProperties.BackupCodesProperties();
        backupCodesProperties.setEnabled(false);
        mfaProperties.setTotp(totpProperties);
        mfaProperties.setBackupCodes(backupCodesProperties);
        when(securityProperties.getMfa()).thenReturn(mfaProperties);

        // Enable breach check without API key
        passwordProperties.setBreachCheckEnabled(true);
        when(environment.getProperty("security.password-policy.breach-check-api"))
            .thenReturn(null);

        // Use Redis session store without Redis config
        SecurityProperties.SessionProperties sessionProperties = 
            new SecurityProperties.SessionProperties();
        sessionProperties.setStoreType("redis");
        when(securityProperties.getSession()).thenReturn(sessionProperties);
        when(environment.getProperty("spring.redis.host")).thenReturn(null);

        // When
        SecurityConfigValidator.ValidationResult result = configValidator.validateConfiguration();

        // Then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).containsKey("mfa.backupCodes.enabled");
        assertThat(result.getErrors()).containsKeys(
            "passwordPolicy.breachCheckApi",
            "session.storeType"
        );
    }

    @Test
    void handleValidationErrors_withStrictMode_throwsException() {
        // Given
        when(validator.validate(any())).thenReturn(new HashSet<>());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        
        // Create a validator with strict enforcement
        SecurityConfigValidator strictValidator = new SecurityConfigValidator();
        ReflectionTestUtils.setField(strictValidator, "enforcementMode", "strict");
        ReflectionTestUtils.setField(strictValidator, "securityProperties", securityProperties);
        ReflectionTestUtils.setField(strictValidator, "validator", validator);
        ReflectionTestUtils.setField(strictValidator, "environment", environment);
        
        // Set invalid production config
        lockoutProperties.setEnabled(false);

        // When/Then
        assertThatThrownBy(() -> strictValidator.validateConfiguration())
            .isInstanceOf(SecurityConfigValidator.SecurityConfigurationException.class)
            .hasMessageContaining("Security configuration validation failed");
    }

    /**
     * Utility class for reflection-based field setting in tests
     */
    static class ReflectionTestUtils {
        static void setField(Object target, String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set field: " + fieldName, e);
            }
        }
    }
}