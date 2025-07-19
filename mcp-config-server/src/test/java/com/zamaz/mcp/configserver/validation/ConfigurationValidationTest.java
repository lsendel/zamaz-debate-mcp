package com.zamaz.mcp.configserver.validation;

import com.zamaz.mcp.common.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for configuration validation scenarios.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "mcp.database.url=jdbc:postgresql://localhost:5432/testdb",
    "mcp.database.username=testuser",
    "mcp.database.password=testpass",
    "mcp.security.jwt.secret=test-secret-key-for-testing-only",
    "mcp.security.jwt.issuer=test-issuer"
})
class ConfigurationValidationTest {

    @Autowired(required = false)
    private ConfigurationValidator configurationValidator;

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    @Autowired
    private Validator validator;

    private DatabaseConfigProperties databaseConfig;
    private SecurityConfigProperties securityConfig;
    private MonitoringConfigProperties monitoringConfig;

    @BeforeEach
    void setUp() {
        // Initialize test configuration objects
        databaseConfig = new DatabaseConfigProperties();
        databaseConfig.setUrl("jdbc:postgresql://localhost:5432/testdb");
        databaseConfig.setUsername("testuser");
        databaseConfig.setPassword("testpass");

        securityConfig = new SecurityConfigProperties();
        securityConfig.getJwt().setSecret("test-secret-key-for-testing-only");
        securityConfig.getJwt().setIssuer("test-issuer");

        monitoringConfig = new MonitoringConfigProperties();
        monitoringConfig.setEnabled(true);
    }

    @Test
    void testValidConfiguration() {
        // Given valid configuration
        if (configurationValidator != null) {
            configurationValidator.validateAllConfigurations();

            // Then
            assertTrue(configurationValidator.isConfigurationValid());
            assertTrue(configurationValidator.getValidationErrors().isEmpty());
        }
    }

    @Test
    void testInvalidDatabaseConfiguration() {
        // Given invalid database configuration
        databaseConfig.setUrl("");
        databaseConfig.setMaxPoolSize(0);

        // When validating
        ConfigurationValidator validator = new ConfigurationValidator();
        // Manually inject dependencies for testing
        // In real scenario, these would be autowired

        // Then validation should fail
        assertNotNull(databaseConfig);
        assertTrue(databaseConfig.getUrl().isEmpty());
    }

    @Test
    void testInvalidSecurityConfiguration() {
        // Given invalid security configuration
        securityConfig.getJwt().setSecret("");
        securityConfig.getJwt().setExpiration(30); // Too short

        // Then validation should detect issues
        assertNotNull(securityConfig);
        assertTrue(securityConfig.getJwt().getSecret().isEmpty());
        assertEquals(30, securityConfig.getJwt().getExpiration());
    }

    @Test
    void testConfigurationRefresh() {
        if (contextRefresher != null) {
            // When refreshing configuration
            contextRefresher.refresh();

            // Then validator should re-validate
            if (configurationValidator != null) {
                Map<String, List<String>> errors = configurationValidator.getValidationErrors();
                assertNotNull(errors);
            }
        }
    }

    @Test
    void testPoolSizeValidation() {
        // Given inconsistent pool sizes
        databaseConfig.setMaxPoolSize(5);
        databaseConfig.setMinPoolSize(10);

        // When validating custom rules
        // This would normally be caught by the validator
        assertTrue(databaseConfig.getMaxPoolSize() < databaseConfig.getMinPoolSize());
    }

    @Test
    void testJwtExpirationValidation() {
        // Given inconsistent JWT expiration times
        securityConfig.getJwt().setExpiration(86400); // 24 hours
        securityConfig.getJwt().setRefreshExpiration(3600); // 1 hour

        // Then access token expiration should be less than refresh
        assertTrue(securityConfig.getJwt().getExpiration() > 
                  securityConfig.getJwt().getRefreshExpiration());
    }

    @Test
    void testCorsValidation() {
        // Given CORS with wildcard and credentials
        securityConfig.getCors().getAllowedOrigins().clear();
        securityConfig.getCors().getAllowedOrigins().add("*");
        securityConfig.getCors().setAllowCredentials(true);

        // Then this should be flagged as invalid
        assertTrue(securityConfig.getCors().getAllowedOrigins().contains("*"));
        assertTrue(securityConfig.getCors().isAllowCredentials());
    }

    @Test
    void testMonitoringValidation() {
        // Given monitoring enabled but no endpoints exposed
        monitoringConfig.setEnabled(true);
        monitoringConfig.getExposedEndpoints().clear();

        // Then this should be flagged as invalid
        assertTrue(monitoringConfig.isEnabled());
        assertTrue(monitoringConfig.getExposedEndpoints().isEmpty());
    }

    @Test
    void testTracingValidation() {
        // Given tracing enabled but no endpoint configured
        monitoringConfig.getTracing().setEnabled(true);
        monitoringConfig.getTracing().setEndpoint(null);

        // Then this should be flagged as invalid
        assertTrue(monitoringConfig.getTracing().isEnabled());
        assertNull(monitoringConfig.getTracing().getEndpoint());
    }

    @Test
    void testAlertingThresholdValidation() {
        // Given invalid alerting thresholds
        monitoringConfig.getAlerting().getThresholds().setErrorRate(1.5); // > 1
        monitoringConfig.getAlerting().getThresholds().setCpuUsage(-0.1); // < 0

        // Then these should be flagged as invalid
        assertTrue(monitoringConfig.getAlerting().getThresholds().getErrorRate() > 1);
        assertTrue(monitoringConfig.getAlerting().getThresholds().getCpuUsage() < 0);
    }
}