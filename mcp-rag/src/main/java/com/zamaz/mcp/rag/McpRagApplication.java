package com.zamaz.mcp.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.rag.repository")
@EntityScan(basePackages = "com.zamaz.mcp.rag.entity")
@EnableJpaAuditing
public class McpRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpRagApplication.class, args);
    }

}
