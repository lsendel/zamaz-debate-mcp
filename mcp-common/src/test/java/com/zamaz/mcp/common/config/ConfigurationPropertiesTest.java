package com.zamaz.mcp.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
    DatabaseConfigProperties.class,
    SecurityConfigProperties.class,
    MonitoringConfigProperties.class,
    CachingConfigProperties.class,
    RateLimitingConfigProperties.class,
    KafkaConfigProperties.class
})
@TestPropertySource(properties = {
    "mcp.database.url=jdbc:postgresql://localhost:5432/testdb",
    "mcp.database.username=testuser",
    "mcp.database.password=testpass",
    "mcp.security.jwt.secret=test-secret-key",
    "mcp.security.jwt.expiration=3600000",
    "mcp.monitoring.metrics.enabled=true",
    "mcp.caching.redis.host=localhost",
    "mcp.caching.redis.port=6379",
    "mcp.rate-limiting.enabled=true",
    "mcp.rate-limiting.requests-per-minute=100",
    "mcp.kafka.bootstrap-servers=localhost:9092"
})
class ConfigurationPropertiesTest {

    @Autowired
    private DatabaseConfigProperties databaseConfig;

    @Autowired
    private SecurityConfigProperties securityConfig;

    @Autowired
    private MonitoringConfigProperties monitoringConfig;

    @Autowired
    private CachingConfigProperties cachingConfig;

    @Autowired
    private RateLimitingConfigProperties rateLimitingConfig;

    @Autowired
    private KafkaConfigProperties kafkaConfig;

    @Autowired
    private Validator validator;

    @Test
    void testDatabaseConfigProperties() {
        assertNotNull(databaseConfig);
        assertEquals("jdbc:postgresql://localhost:5432/testdb", databaseConfig.getUrl());
        assertEquals("testuser", databaseConfig.getUsername());
        assertEquals("testpass", databaseConfig.getPassword());
        
        // Test validation
        Set<ConstraintViolation<DatabaseConfigProperties>> violations = validator.validate(databaseConfig);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testSecurityConfigProperties() {
        assertNotNull(securityConfig);
        assertNotNull(securityConfig.getJwt());
        assertEquals("test-secret-key", securityConfig.getJwt().getSecret());
        assertEquals(3600000L, securityConfig.getJwt().getExpiration());
        
        Set<ConstraintViolation<SecurityConfigProperties>> violations = validator.validate(securityConfig);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMonitoringConfigProperties() {
        assertNotNull(monitoringConfig);
        assertNotNull(monitoringConfig.getMetrics());
        assertTrue(monitoringConfig.getMetrics().isEnabled());
    }

    @Test
    void testCachingConfigProperties() {
        assertNotNull(cachingConfig);
        assertNotNull(cachingConfig.getRedis());
        assertEquals("localhost", cachingConfig.getRedis().getHost());
        assertEquals(6379, cachingConfig.getRedis().getPort());
    }

    @Test
    void testRateLimitingConfigProperties() {
        assertNotNull(rateLimitingConfig);
        assertTrue(rateLimitingConfig.isEnabled());
        assertEquals(100, rateLimitingConfig.getRequestsPerMinute());
    }

    @Test
    void testKafkaConfigProperties() {
        assertNotNull(kafkaConfig);
        assertEquals("localhost:9092", kafkaConfig.getBootstrapServers());
    }

    @Test
    void testDatabaseConfigValidation() {
        DatabaseConfigProperties invalidConfig = new DatabaseConfigProperties();
        // Don't set required fields
        
        Set<ConstraintViolation<DatabaseConfigProperties>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Database URL is required")));
    }

    @Test
    void testSecurityConfigValidation() {
        SecurityConfigProperties invalidConfig = new SecurityConfigProperties();
        SecurityConfigProperties.JwtProperties jwt = new SecurityConfigProperties.JwtProperties();
        jwt.setExpiration(-1L); // Invalid expiration
        invalidConfig.setJwt(jwt);
        
        Set<ConstraintViolation<SecurityConfigProperties>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
    }
}