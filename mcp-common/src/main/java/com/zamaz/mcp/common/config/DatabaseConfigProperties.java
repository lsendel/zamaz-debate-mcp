package com.zamaz.mcp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Common database configuration properties used across all MCP microservices.
 * These properties are loaded from the centralized configuration server.
 */
@ConfigurationProperties(prefix = "mcp.database")
@Validated
public class DatabaseConfigProperties {

    /**
     * Database connection URL
     */
    @NotBlank(message = "Database URL is required")
    private String url;

    /**
     * Database username
     */
    @NotBlank(message = "Database username is required")
    private String username;

    /**
     * Database password (should be encrypted in configuration)
     */
    @NotBlank(message = "Database password is required")
    private String password;

    /**
     * Database driver class name
     */
    private String driverClassName = "org.postgresql.Driver";

    /**
     * Maximum pool size for database connections
     */
    @Min(value = 1, message = "Max pool size must be at least 1")
    private int maxPoolSize = 10;

    /**
     * Minimum idle connections in the pool
     */
    @Min(value = 0, message = "Min pool size cannot be negative")
    private int minPoolSize = 2;

    /**
     * Connection timeout in milliseconds
     */
    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    private long connectionTimeout = 30000;

    /**
     * Idle timeout in milliseconds
     */
    @Min(value = 10000, message = "Idle timeout must be at least 10000ms")
    private long idleTimeout = 600000;

    /**
     * Maximum lifetime of a connection in milliseconds
     */
    @Min(value = 30000, message = "Max lifetime must be at least 30000ms")
    private long maxLifetime = 1800000;

    /**
     * Validation query to test connections
     */
    private String validationQuery = "SELECT 1";

    /**
     * Whether to test connections on borrow
     */
    private boolean testOnBorrow = true;

    /**
     * Whether to test connections while idle
     */
    private boolean testWhileIdle = true;

    /**
     * Time between eviction runs in milliseconds
     */
    private long timeBetweenEvictionRuns = 30000;

    /**
     * Leak detection threshold in milliseconds
     */
    private long leakDetectionThreshold = 60000;

    /**
     * Whether to enable JMX monitoring
     */
    private boolean jmxEnabled = true;

    /**
     * Connection pool name
     */
    private String poolName;

    /**
     * Whether to cache prepared statements
     */
    private boolean cachePrepStmts = true;

    /**
     * Prepared statement cache size
     */
    private int prepStmtCacheSize = 250;

    /**
     * Prepared statement cache SQL limit
     */
    private int prepStmtCacheSqlLimit = 2048;

    // Getters and setters

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public long getTimeBetweenEvictionRuns() {
        return timeBetweenEvictionRuns;
    }

    public void setTimeBetweenEvictionRuns(long timeBetweenEvictionRuns) {
        this.timeBetweenEvictionRuns = timeBetweenEvictionRuns;
    }

    public long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(long leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public void setCachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
    }

    public int getPrepStmtCacheSize() {
        return prepStmtCacheSize;
    }

    public void setPrepStmtCacheSize(int prepStmtCacheSize) {
        this.prepStmtCacheSize = prepStmtCacheSize;
    }

    public int getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public void setPrepStmtCacheSqlLimit(int prepStmtCacheSqlLimit) {
        this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
    }
}