package com.zamaz.mcp.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityConfigProperties.
 */
class SecurityConfigPropertiesTest {

    private LocalValidatorFactoryBean validator;
    private SecurityConfigProperties properties;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        properties = new SecurityConfigProperties();
    }

    @Test
    void testValidJwtConfiguration() {
        // Given
        SecurityConfigProperties.Jwt jwt = properties.getJwt();
        jwt.setSecret("mySecretKey123456789012345678901");
        jwt.setExpiration(3600);
        jwt.setRefreshExpiration(86400);
        jwt.setIssuer("test-issuer");

        // When
        Errors errors = new BeanPropertyBindingResult(jwt, "jwt");
        validator.validate(jwt, errors);

        // Then
        assertFalse(errors.hasErrors());
    }

    @Test
    void testInvalidJwtConfiguration_MissingSecret() {
        // Given
        SecurityConfigProperties.Jwt jwt = properties.getJwt();
        jwt.setSecret("");
        jwt.setIssuer("test-issuer");

        // When
        Errors errors = new BeanPropertyBindingResult(jwt, "jwt");
        validator.validate(jwt, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("secret"));
    }

    @Test
    void testInvalidJwtConfiguration_ShortExpiration() {
        // Given
        SecurityConfigProperties.Jwt jwt = properties.getJwt();
        jwt.setSecret("mySecretKey");
        jwt.setExpiration(30); // Too short
        jwt.setIssuer("test-issuer");

        // When
        Errors errors = new BeanPropertyBindingResult(jwt, "jwt");
        validator.validate(jwt, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("expiration"));
    }

    @Test
    void testCorsConfiguration() {
        // Given
        SecurityConfigProperties.Cors cors = properties.getCors();
        cors.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://app.example.com"));
        cors.setAllowedMethods(Arrays.asList("GET", "POST"));
        cors.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        cors.setExposedHeaders(Arrays.asList("X-Total-Count"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(7200);

        // When
        Errors errors = new BeanPropertyBindingResult(cors, "cors");
        validator.validate(cors, errors);

        // Then
        assertFalse(errors.hasErrors());
        assertEquals(2, cors.getAllowedOrigins().size());
        assertEquals(2, cors.getAllowedMethods().size());
        assertEquals(2, cors.getAllowedHeaders().size());
        assertEquals(1, cors.getExposedHeaders().size());
        assertFalse(cors.isAllowCredentials());
        assertEquals(7200, cors.getMaxAge());
    }

    @Test
    void testOAuth2Configuration() {
        // Given
        SecurityConfigProperties.OAuth2 oauth2 = properties.getOauth2();
        oauth2.setIssuerUri("https://auth.example.com");
        oauth2.setJwkSetUri("https://auth.example.com/.well-known/jwks.json");
        oauth2.setClientId("client-id");
        oauth2.setClientSecret("client-secret");
        oauth2.setRedirectUri("http://localhost:3000/callback");
        oauth2.setScopes(Arrays.asList("openid", "profile"));

        // Then
        assertEquals("https://auth.example.com", oauth2.getIssuerUri());
        assertEquals("https://auth.example.com/.well-known/jwks.json", oauth2.getJwkSetUri());
        assertEquals("client-id", oauth2.getClientId());
        assertEquals("client-secret", oauth2.getClientSecret());
        assertEquals("http://localhost:3000/callback", oauth2.getRedirectUri());
        assertEquals(2, oauth2.getScopes().size());
    }

    @Test
    void testSecurityHeadersConfiguration() {
        // Given
        SecurityConfigProperties.Headers headers = properties.getHeaders();
        headers.setContentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'");
        headers.setxFrameOptions("SAMEORIGIN");
        headers.setxContentTypeOptions("nosniff");
        headers.setxXssProtection("1; mode=block");
        headers.setStrictTransportSecurity("max-age=63072000; includeSubDomains; preload");
        headers.setReferrerPolicy("no-referrer");
        headers.setPermissionsPolicy("geolocation=()");

        // Then
        assertEquals("default-src 'self'; script-src 'self' 'unsafe-inline'", headers.getContentSecurityPolicy());
        assertEquals("SAMEORIGIN", headers.getxFrameOptions());
        assertEquals("nosniff", headers.getxContentTypeOptions());
        assertEquals("1; mode=block", headers.getxXssProtection());
        assertEquals("max-age=63072000; includeSubDomains; preload", headers.getStrictTransportSecurity());
        assertEquals("no-referrer", headers.getReferrerPolicy());
        assertEquals("geolocation=()", headers.getPermissionsPolicy());
    }

    @Test
    void testRateLimitConfiguration() {
        // Given
        SecurityConfigProperties.RateLimit rateLimit = properties.getRateLimit();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerMinute(100);
        rateLimit.setRequestsPerHour(5000);
        rateLimit.setRequestsPerDay(50000);
        rateLimit.setStrategy("SLIDING_WINDOW");

        // Then
        assertTrue(rateLimit.isEnabled());
        assertEquals(100, rateLimit.getRequestsPerMinute());
        assertEquals(5000, rateLimit.getRequestsPerHour());
        assertEquals(50000, rateLimit.getRequestsPerDay());
        assertEquals("SLIDING_WINDOW", rateLimit.getStrategy());
    }

    @Test
    void testDefaultValues() {
        // JWT defaults
        SecurityConfigProperties.Jwt jwt = properties.getJwt();
        assertEquals(86400, jwt.getExpiration());
        assertEquals(604800, jwt.getRefreshExpiration());
        assertEquals("zamaz-mcp", jwt.getIssuer());
        assertEquals("mcp-services", jwt.getAudience());
        assertEquals(60, jwt.getClockSkew());
        assertTrue(jwt.isRequireExpiration());
        assertEquals("HS512", jwt.getAlgorithm());

        // CORS defaults
        SecurityConfigProperties.Cors cors = properties.getCors();
        assertEquals(Arrays.asList("http://localhost:3000"), cors.getAllowedOrigins());
        assertEquals(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"), cors.getAllowedMethods());
        assertEquals(Arrays.asList("*"), cors.getAllowedHeaders());
        assertTrue(cors.isAllowCredentials());
        assertEquals(3600, cors.getMaxAge());

        // Headers defaults
        SecurityConfigProperties.Headers headers = properties.getHeaders();
        assertEquals("default-src 'self'", headers.getContentSecurityPolicy());
        assertEquals("DENY", headers.getxFrameOptions());
        assertEquals("nosniff", headers.getxContentTypeOptions());
        assertEquals("1; mode=block", headers.getxXssProtection());

        // Rate limit defaults
        SecurityConfigProperties.RateLimit rateLimit = properties.getRateLimit();
        assertTrue(rateLimit.isEnabled());
        assertEquals(60, rateLimit.getRequestsPerMinute());
        assertEquals(1000, rateLimit.getRequestsPerHour());
        assertEquals(10000, rateLimit.getRequestsPerDay());
        assertEquals("TOKEN_BUCKET", rateLimit.getStrategy());
    }

    @Test
    void testNestedPropertiesValidation() {
        // Given
        properties.getJwt().setSecret("validSecret");
        properties.getJwt().setIssuer("validIssuer");
        properties.getCors().setMaxAge(-1); // Invalid

        // When
        Errors errors = new BeanPropertyBindingResult(properties.getCors(), "cors");
        validator.validate(properties.getCors(), errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("maxAge"));
    }
}