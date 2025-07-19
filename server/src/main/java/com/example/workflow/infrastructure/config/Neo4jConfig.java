package com.example.workflow.infrastructure.config;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Neo4j configuration for workflow storage
 * Configures Neo4j driver, repositories, and initializes constraints
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.example.workflow.infrastructure.neo4j")
@EnableTransactionManagement
public class Neo4jConfig extends AbstractNeo4jConfig {
    
    @Value("${workflow.neo4j.init-constraints:true}")
    private boolean initConstraints;
    
    @Value("${workflow.neo4j.batch-size:1000}")
    private int batchSize;
    
    private final Driver driver;
    
    public Neo4jConfig(Driver driver) {
        this.driver = driver;
    }
    
    @Override
    public Driver driver() {
        return driver;
    }
    
    @Bean
    public Neo4jTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {
        return new Neo4jTransactionManager(driver, databaseNameProvider);
    }
    
    /**
     * Initialize Neo4j constraints and indexes after application startup
     */
    @PostConstruct
    public void initializeConstraints() {
        if (initConstraints) {
            try {
                executeConstraintsScript();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Neo4j constraints", e);
            }
        }
    }
    
    /**
     * Execute the constraints initialization script
     */
    private void executeConstraintsScript() throws IOException {
        Path constraintsPath = Paths.get("src/main/resources/neo4j/init-constraints.cypher");
        
        // Try to read from classpath if file doesn't exist in filesystem
        if (!Files.exists(constraintsPath)) {
            constraintsPath = Paths.get(getClass().getClassLoader()
                .getResource("neo4j/init-constraints.cypher").getPath());
        }
        
        if (Files.exists(constraintsPath)) {
            String constraintsScript = Files.readString(constraintsPath);
            executeConstraints(constraintsScript);
        }
    }
    
    /**
     * Execute constraint creation queries
     */
    private void executeConstraints(String script) {
        try (var session = driver.session()) {
            // Split script by semicolons and execute each statement
            String[] statements = script.split(";");
            
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                    try {
                        session.run(trimmed);
                    } catch (Exception e) {
                        // Log warning but continue - constraints might already exist
                        System.out.println("Warning: Failed to execute constraint: " + trimmed + 
                                         " - " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Get configured batch size for bulk operations
     */
    public int getBatchSize() {
        return batchSize;
    }
}