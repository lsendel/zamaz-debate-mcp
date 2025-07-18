package com.zamaz.mcp.rag.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for RAG service.
 */
@Configuration
public class SwaggerConfiguration {
    
    @Value("${server.port:5004}")
    private String serverPort;
    
    @Bean
    public OpenAPI ragOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MCP RAG Service API")
                .version("1.0.0")
                .description("Retrieval Augmented Generation service for document processing and semantic search")
                .contact(new Contact()
                    .name("MCP Team")
                    .email("team@mcp.com")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local development server"),
                new Server()
                    .url("https://api.mcp.com/rag")
                    .description("Production server")
            ));
    }
}