package com.example.workflow.infrastructure.config;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Neo4j configuration for workflow graph storage
 * Configures connection, repositories, and transaction management
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.example.workflow.infrastructure.neo4j")
@EnableTransactionManagement
public class Neo4jConfig extends AbstractNeo4jConfig {
    
    @Value("${spring.neo4j.uri:bolt://localhost:7687}")
    private String neo4jUri;
    
    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String neo4jUsername;
    
    @Value("${spring.neo4j.authentication.password:password}")
    private String neo4jPassword;
    
    @Override
    public Driver driver() {
        return org.neo4j.driver.GraphDatabase.driver(neo4jUri, 
            org.neo4j.driver.AuthTokens.basic(neo4jUsername, neo4jPassword));
    }
    
    @Override
    protected java.util.Collection<String> getMappingBasePackages() {
        return java.util.List.of("com.example.workflow.infrastructure.neo4j");
    }
    
    /**
     * Custom Neo4j session factory configuration
     */
    @Bean
    public org.springframework.data.neo4j.core.Neo4jTemplate neo4jTemplate(Driver driver) {
        return new org.springframework.data.neo4j.core.Neo4jTemplate(driver);
    }
}