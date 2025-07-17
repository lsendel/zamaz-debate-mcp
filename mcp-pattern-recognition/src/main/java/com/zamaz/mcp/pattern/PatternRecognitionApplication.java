package com.zamaz.mcp.pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Pattern Recognition System.
 * 
 * This service provides comprehensive codebase analysis capabilities including:
 * - Design pattern detection (GoF, enterprise, architectural patterns)
 * - Code smell detection and classification with severity scoring
 * - Anti-pattern recognition (god class, spaghetti code, etc.)
 * - Team-specific pattern learning with ML capabilities
 * - Custom pattern definition and recognition
 * - Pattern-based suggestion and recommendation system
 * - Performance optimization for large codebases
 * - Detailed reporting and visualization
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zamaz.mcp.pattern",
    "com.zamaz.mcp.common"
})
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.pattern.repository")
@EnableAsync
@EnableScheduling
public class PatternRecognitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatternRecognitionApplication.class, args);
    }
}