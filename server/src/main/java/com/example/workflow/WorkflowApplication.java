package com.example.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application for Workflow Editor services
 * Implements hexagonal architecture with GraphQL API
 */
@SpringBootApplication(scanBasePackages = {
    "com.example.workflow",
    "com.zamaz.mcp.common",
    "com.zamaz.mcp.security"
})
@EnableJpaRepositories(basePackages = {
    "com.example.workflow.data.adapters.jpa",
    "com.zamaz.mcp.common.repository"
})
@EnableNeo4jRepositories(basePackages = {
    "com.example.workflow.data.adapters.neo4j"
})
@EntityScan(basePackages = {
    "com.example.workflow.data.entities",
    "com.zamaz.mcp.common.entity"
})
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}