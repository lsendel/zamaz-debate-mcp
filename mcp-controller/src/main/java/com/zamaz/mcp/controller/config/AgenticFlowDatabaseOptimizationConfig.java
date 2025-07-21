package com.zamaz.mcp.controller.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Database optimization configuration for agentic flow operations.
 * Configures connection pooling and query optimizations.
 */
@Configuration
public class AgenticFlowDatabaseOptimizationConfig {
    
    @Value("${agentic-flow.db.pool-size:20}")
    private int poolSize;
    
    @Value("${agentic-flow.db.max-lifetime:1800000}")
    private long maxLifetime;
    
    @Value("${agentic-flow.db.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${agentic-flow.db.idle-timeout:600000}")
    private long idleTimeout;
    
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = "agentic-flow.db.optimization.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public DataSource optimizedDataSource(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        // Connection pool settings
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(poolSize / 2);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        
        // Performance optimizations
        config.setAutoCommit(false);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("AgenticFlowPool");
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        
        // Connection pool monitoring
        config.setRegisterMbeans(true);
        
        return new HikariDataSource(config);
    }
    
    /**
     * Read-only data source for analytics queries.
     */
    @Bean(name = "analyticsDataSource")
    @ConditionalOnProperty(
        name = "agentic-flow.db.read-replica.enabled",
        havingValue = "true"
    )
    public DataSource analyticsDataSource(
            @Value("${agentic-flow.db.read-replica.url}") String jdbcUrl,
            @Value("${agentic-flow.db.read-replica.username}") String username,
            @Value("${agentic-flow.db.read-replica.password}") String password) {
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        // Smaller pool for read-only operations
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setReadOnly(true);
        config.setPoolName("AgenticFlowAnalyticsPool");
        
        // Longer timeouts for analytics queries
        config.setConnectionTimeout(60000);
        
        return new HikariDataSource(config);
    }
}