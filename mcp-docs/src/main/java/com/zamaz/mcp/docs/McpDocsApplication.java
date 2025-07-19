package com.zamaz.mcp.docs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.zamaz.mcp.docs", "com.zamaz.mcp.security", "com.zamaz.mcp.common"})
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.docs.repository")
@EntityScan(basePackages = "com.zamaz.mcp.docs.entity")
public class McpDocsApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpDocsApplication.class, args);
    }
}