package com.zamaz.mcp.configserver.validation;

import com.zamaz.mcp.configserver.validator.ConfigurationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidationTest {

    private ConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigurationValidator();
    }

    @Test
    void testValidConfiguration() {
        Environment environment = createTestEnvironment(
                "test-app",
                "default",
                createValidPropertySource()
        );

        assertTrue(validator.isValid(environment));
        assertTrue(validator.getValidationErrors().isEmpty());
    }

    @Test
    void testInvalidConfiguration() {
        Environment environment = createTestEnvironment(
                "test-app",
                "default",
                createInvalidPropertySource()
        );

        assertFalse(validator.isValid(environment));
        assertFalse(validator.getValidationErrors().isEmpty());
    }

    @Test
    void testMissingRequiredProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Missing required database URL
        properties.put("spring.datasource.username", "user");
        properties.put("spring.datasource.password", "pass");

        PropertySource propertySource = new PropertySource("test", properties);
        Environment environment = createTestEnvironment("test-app", "default", propertySource);

        assertFalse(validator.isValid(environment));
        assertTrue(validator.getValidationErrors().stream()
                .anyMatch(error -> error.contains("database URL")));
    }

    @Test
    void testSensitiveDataValidation() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/db");
        properties.put("spring.datasource.password", "plain-text-password"); // Not encrypted
        properties.put("api.key", "unencrypted-api-key"); // Not encrypted

        PropertySource propertySource = new PropertySource("test", properties);
        Environment environment = createTestEnvironment("test-app", "default", propertySource);

        assertFalse(validator.isValid(environment));
        assertTrue(validator.getValidationErrors().stream()
                .anyMatch(error -> error.contains("should be encrypted")));
    }

    @Test
    void testEncryptedPropertiesValidation() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/db");
        properties.put("spring.datasource.password", "{cipher}encrypted-password");
        properties.put("api.key", "{cipher}encrypted-api-key");

        PropertySource propertySource = new PropertySource("test", properties);
        Environment environment = createTestEnvironment("test-app", "default", propertySource);

        assertTrue(validator.isValid(environment));
    }

    @Test
    void testProfileSpecificValidation() {
        // Development profile - less strict validation
        Environment devEnvironment = createTestEnvironment(
                "test-app",
                "development",
                createDevPropertySource()
        );
        assertTrue(validator.isValid(devEnvironment));

        // Production profile - strict validation
        Environment prodEnvironment = createTestEnvironment(
                "test-app",
                "production",
                createDevPropertySource() // Using dev properties for prod should fail
        );
        assertFalse(validator.isValid(prodEnvironment));
    }

    private Environment createTestEnvironment(String name, String profile, PropertySource... sources) {
        Environment environment = new Environment(name, new String[]{profile});
        environment.addAll(Arrays.asList(sources));
        return environment;
    }

    private PropertySource createValidPropertySource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/db");
        properties.put("spring.datasource.username", "user");
        properties.put("spring.datasource.password", "{cipher}encrypted-password");
        properties.put("server.port", "8080");
        return new PropertySource("valid", properties);
    }

    private PropertySource createInvalidPropertySource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", "invalid-url");
        properties.put("server.port", "invalid-port");
        return new PropertySource("invalid", properties);
    }

    private PropertySource createDevPropertySource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", "jdbc:h2:mem:devdb");
        properties.put("spring.datasource.username", "sa");
        properties.put("spring.datasource.password", "");
        properties.put("debug", "true");
        return new PropertySource("dev", properties);
    }
}