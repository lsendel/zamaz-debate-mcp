package com.zamaz.mcp.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatabaseConfigProperties.
 */
class DatabaseConfigPropertiesTest {

    private LocalValidatorFactoryBean validator;
    private DatabaseConfigProperties properties;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        properties = new DatabaseConfigProperties();
    }

    @Test
    void testValidConfiguration() {
        // Given
        properties.setUrl("jdbc:postgresql://localhost:5432/testdb");
        properties.setUsername("testuser");
        properties.setPassword("testpass");
        properties.setMaxPoolSize(10);
        properties.setMinPoolSize(2);
        properties.setConnectionTimeout(30000);

        // When
        Errors errors = new BeanPropertyBindingResult(properties, "properties");
        validator.validate(properties, errors);

        // Then
        assertFalse(errors.hasErrors());
    }

    @Test
    void testInvalidConfiguration_MissingUrl() {
        // Given
        properties.setUrl("");
        properties.setUsername("testuser");
        properties.setPassword("testpass");

        // When
        Errors errors = new BeanPropertyBindingResult(properties, "properties");
        validator.validate(properties, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("url"));
    }

    @Test
    void testInvalidConfiguration_MissingUsername() {
        // Given
        properties.setUrl("jdbc:postgresql://localhost:5432/testdb");
        properties.setUsername("");
        properties.setPassword("testpass");

        // When
        Errors errors = new BeanPropertyBindingResult(properties, "properties");
        validator.validate(properties, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("username"));
    }

    @Test
    void testInvalidConfiguration_InvalidPoolSize() {
        // Given
        properties.setUrl("jdbc:postgresql://localhost:5432/testdb");
        properties.setUsername("testuser");
        properties.setPassword("testpass");
        properties.setMaxPoolSize(0); // Invalid

        // When
        Errors errors = new BeanPropertyBindingResult(properties, "properties");
        validator.validate(properties, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("maxPoolSize"));
    }

    @Test
    void testInvalidConfiguration_InvalidTimeout() {
        // Given
        properties.setUrl("jdbc:postgresql://localhost:5432/testdb");
        properties.setUsername("testuser");
        properties.setPassword("testpass");
        properties.setConnectionTimeout(500); // Too low

        // When
        Errors errors = new BeanPropertyBindingResult(properties, "properties");
        validator.validate(properties, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("connectionTimeout"));
    }

    @Test
    void testDefaultValues() {
        // Then
        assertEquals("org.postgresql.Driver", properties.getDriverClassName());
        assertEquals(10, properties.getMaxPoolSize());
        assertEquals(2, properties.getMinPoolSize());
        assertEquals(30000, properties.getConnectionTimeout());
        assertEquals(600000, properties.getIdleTimeout());
        assertEquals(1800000, properties.getMaxLifetime());
        assertEquals("SELECT 1", properties.getValidationQuery());
        assertTrue(properties.isTestOnBorrow());
        assertTrue(properties.isTestWhileIdle());
        assertEquals(30000, properties.getTimeBetweenEvictionRuns());
        assertEquals(60000, properties.getLeakDetectionThreshold());
        assertTrue(properties.isJmxEnabled());
        assertTrue(properties.isCachePrepStmts());
        assertEquals(250, properties.getPrepStmtCacheSize());
        assertEquals(2048, properties.getPrepStmtCacheSqlLimit());
    }

    @Test
    void testGettersAndSetters() {
        // Given
        String url = "jdbc:postgresql://prod:5432/proddb";
        String username = "produser";
        String password = "prodpass";
        String driverClassName = "org.postgresql.Driver";
        int maxPoolSize = 20;
        int minPoolSize = 5;
        long connectionTimeout = 60000;
        long idleTimeout = 900000;
        long maxLifetime = 3600000;
        String validationQuery = "SELECT 1 FROM DUAL";
        boolean testOnBorrow = false;
        boolean testWhileIdle = false;
        long timeBetweenEvictionRuns = 60000;
        long leakDetectionThreshold = 120000;
        boolean jmxEnabled = false;
        String poolName = "MyPool";
        boolean cachePrepStmts = false;
        int prepStmtCacheSize = 500;
        int prepStmtCacheSqlLimit = 4096;

        // When
        properties.setUrl(url);
        properties.setUsername(username);
        properties.setPassword(password);
        properties.setDriverClassName(driverClassName);
        properties.setMaxPoolSize(maxPoolSize);
        properties.setMinPoolSize(minPoolSize);
        properties.setConnectionTimeout(connectionTimeout);
        properties.setIdleTimeout(idleTimeout);
        properties.setMaxLifetime(maxLifetime);
        properties.setValidationQuery(validationQuery);
        properties.setTestOnBorrow(testOnBorrow);
        properties.setTestWhileIdle(testWhileIdle);
        properties.setTimeBetweenEvictionRuns(timeBetweenEvictionRuns);
        properties.setLeakDetectionThreshold(leakDetectionThreshold);
        properties.setJmxEnabled(jmxEnabled);
        properties.setPoolName(poolName);
        properties.setCachePrepStmts(cachePrepStmts);
        properties.setPrepStmtCacheSize(prepStmtCacheSize);
        properties.setPrepStmtCacheSqlLimit(prepStmtCacheSqlLimit);

        // Then
        assertEquals(url, properties.getUrl());
        assertEquals(username, properties.getUsername());
        assertEquals(password, properties.getPassword());
        assertEquals(driverClassName, properties.getDriverClassName());
        assertEquals(maxPoolSize, properties.getMaxPoolSize());
        assertEquals(minPoolSize, properties.getMinPoolSize());
        assertEquals(connectionTimeout, properties.getConnectionTimeout());
        assertEquals(idleTimeout, properties.getIdleTimeout());
        assertEquals(maxLifetime, properties.getMaxLifetime());
        assertEquals(validationQuery, properties.getValidationQuery());
        assertEquals(testOnBorrow, properties.isTestOnBorrow());
        assertEquals(testWhileIdle, properties.isTestWhileIdle());
        assertEquals(timeBetweenEvictionRuns, properties.getTimeBetweenEvictionRuns());
        assertEquals(leakDetectionThreshold, properties.getLeakDetectionThreshold());
        assertEquals(jmxEnabled, properties.isJmxEnabled());
        assertEquals(poolName, properties.getPoolName());
        assertEquals(cachePrepStmts, properties.isCachePrepStmts());
        assertEquals(prepStmtCacheSize, properties.getPrepStmtCacheSize());
        assertEquals(prepStmtCacheSqlLimit, properties.getPrepStmtCacheSqlLimit());
    }
}