package com.zamaz.mcp.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Optimized database configuration with HikariCP connection pooling
 */
@Configuration
@Slf4j
public class DatabaseConfig {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    /**
     * Optimized HikariCP configuration
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();
        
        // Pool sizing - optimized for container environments
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        
        // Timeout configuration
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(5));
        
        // Validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(5));
        
        // Performance optimizations
        config.setAutoCommit(false);
        config.setRegisterMbeans(true);
        config.setPoolName(applicationName + "-db-pool");
        
        // Connection properties for PostgreSQL optimization
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("useLocalTransactionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("stringtype", "unspecified");
        config.addDataSourceProperty("defaultRowFetchSize", "100");
        
        // Enable connection leak detection in development
        if ("dev".equals(System.getProperty("spring.profiles.active"))) {
            config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(60));
        }
        
        return config;
    }
    
    /**
     * Primary datasource with optimized configuration
     */
    @Bean
    @Primary
    public DataSource dataSource(HikariConfig hikariConfig) {
        log.info("Creating HikariCP datasource with pool name: {}", hikariConfig.getPoolName());
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * Read-only datasource for read replicas (if configured)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.read-only.url")
    @ConfigurationProperties(prefix = "spring.datasource.read-only")
    public DataSource readOnlyDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Smaller pool for read-only connections
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        
        // Read-only specific settings
        config.setReadOnly(true);
        config.setAutoCommit(true);
        config.setPoolName(applicationName + "-readonly-pool");
        
        // Same optimization properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(config);
    }
    
    /**
     * Monitoring configuration for connection pool
     */
    @Bean
    public HikariPoolMonitor hikariPoolMonitor() {
        return new HikariPoolMonitor();
    }
    
    /**
     * Connection pool monitor for metrics collection
     */
    public static class HikariPoolMonitor {
        
        public void logPoolStats(HikariDataSource dataSource) {
            if (dataSource != null && dataSource.getHikariPoolMXBean() != null) {
                var pool = dataSource.getHikariPoolMXBean();
                log.debug("Pool stats - Active: {}, Idle: {}, Total: {}, Waiting: {}", 
                    pool.getActiveConnections(),
                    pool.getIdleConnections(),
                    pool.getTotalConnections(),
                    pool.getThreadsAwaitingConnection()
                );
            }
        }
    }
}