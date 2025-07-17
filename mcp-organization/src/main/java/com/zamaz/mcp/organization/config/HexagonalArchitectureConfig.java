package com.zamaz.mcp.organization.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for hexagonal architecture components.
 * This class wires together the domain, application, and adapter layers.
 */
@Configuration
public class HexagonalArchitectureConfig {
    
    /**
     * Provides ObjectMapper for JSON serialization/deserialization.
     * Used by persistence mappers for settings serialization.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Registers JSR-310 time module
        return mapper;
    }
    
    /**
     * Provides Bean Validator for validation service.
     * Used by the application layer for command validation.
     */
    @Bean
    public Validator validator() {
        return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
    }
}