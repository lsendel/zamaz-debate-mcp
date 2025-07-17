package com.zamaz.mcp.common.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates API documentation with versioning information
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiVersionDocumentationGenerator {

    private final ApiVersionConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate API versioning documentation
     */
    public ApiVersionDocumentation generateDocumentation() {
        return ApiVersionDocumentation.builder()
            .currentVersion(config.getCurrentVersion())
            .defaultVersion(config.getDefaultVersion())
            .latestVersion(config.getLatestVersion())
            .supportedVersions(generateVersionDetails())
            .versioningStrategy(config.getStrategy())
            .versionHeader(config.getVersionHeader())
            .versionParameter(config.getVersionParameter())
            .pathPrefix(config.getPathPrefix())
            .mediaTypeTemplate(config.getMediaTypeTemplate())
            .migrationGuides(generateMigrationGuides())
            .examples(generateExamples())
            .generatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Generate version details
     */
    private List<VersionDetail> generateVersionDetails() {
        return config.getSupportedVersions().stream()
            .map(version -> VersionDetail.builder()
                .version(version)
                .deprecated(config.isDeprecatedVersion(version))
                .current(version.equals(config.getCurrentVersion()))
                .latest(version.equals(config.getLatestVersion()))
                .deprecationMessage(config.isDeprecatedVersion(version) ? 
                    getDeprecationMessage(version) : null)
                .supportedUntil(config.isDeprecatedVersion(version) ? 
                    "See migration guide" : null)
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Generate migration guides
     */
    private Map<String, MigrationGuide> generateMigrationGuides() {
        Map<String, MigrationGuide> guides = new HashMap<>();
        
        // Example migration guide
        guides.put("v1-to-v2", MigrationGuide.builder()
            .fromVersion("1")
            .toVersion("2")
            .title("Migration from API v1 to v2")
            .description("This guide helps you migrate from API version 1 to version 2.")
            .breakingChanges(List.of(
                "Response format changed from XML to JSON",
                "Field 'id' renamed to 'identifier'",
                "Removed deprecated endpoint /api/v1/legacy"
            ))
            .newFeatures(List.of(
                "Added pagination support",
                "Improved error handling",
                "New authentication methods"
            ))
            .codeExamples(generateCodeExamples())
            .build());
        
        return guides;
    }

    /**
     * Generate usage examples
     */
    private Map<String, Object> generateExamples() {
        Map<String, Object> examples = new HashMap<>();
        
        // Header-based versioning
        examples.put("header", Map.of(
            "description", "Specify version using header",
            "example", "curl -H \"X-API-Version: 1\" https://api.example.com/users"
        ));
        
        // Path-based versioning
        examples.put("path", Map.of(
            "description", "Specify version in URL path",
            "example", "curl https://api.example.com/api/v1/users"
        ));
        
        // Query parameter versioning
        examples.put("query", Map.of(
            "description", "Specify version as query parameter",
            "example", "curl https://api.example.com/api/users?version=1"
        ));
        
        // Content negotiation
        examples.put("content-type", Map.of(
            "description", "Specify version via Accept header",
            "example", "curl -H \"Accept: application/vnd.mcp.v1+json\" https://api.example.com/users"
        ));
        
        return examples;
    }

    /**
     * Generate code examples for migration
     */
    private Map<String, String> generateCodeExamples() {
        Map<String, String> examples = new HashMap<>();
        
        examples.put("curl_v1", 
            "curl -H \"X-API-Version: 1\" https://api.example.com/users");
        examples.put("curl_v2", 
            "curl -H \"X-API-Version: 2\" https://api.example.com/users");
        
        examples.put("javascript_v1", 
            "fetch('/api/users', { headers: { 'X-API-Version': '1' } })");
        examples.put("javascript_v2", 
            "fetch('/api/users', { headers: { 'X-API-Version': '2' } })");
        
        return examples;
    }

    /**
     * Get deprecation message for version
     */
    private String getDeprecationMessage(String version) {
        return String.format(
            "Version %s is deprecated. Please migrate to version %s. " +
            "See migration guide for details.",
            version, config.getLatestVersion()
        );
    }

    /**
     * API version documentation model
     */
    @lombok.Data
    @lombok.Builder
    public static class ApiVersionDocumentation {
        private String currentVersion;
        private String defaultVersion;
        private String latestVersion;
        private List<VersionDetail> supportedVersions;
        private ApiVersionConfig.VersioningStrategy versioningStrategy;
        private String versionHeader;
        private String versionParameter;
        private String pathPrefix;
        private String mediaTypeTemplate;
        private Map<String, MigrationGuide> migrationGuides;
        private Map<String, Object> examples;
        private LocalDateTime generatedAt;
    }

    /**
     * Version detail model
     */
    @lombok.Data
    @lombok.Builder
    public static class VersionDetail {
        private String version;
        private boolean deprecated;
        private boolean current;
        private boolean latest;
        private String deprecationMessage;
        private String supportedUntil;
    }

    /**
     * Migration guide model
     */
    @lombok.Data
    @lombok.Builder
    public static class MigrationGuide {
        private String fromVersion;
        private String toVersion;
        private String title;
        private String description;
        private List<String> breakingChanges;
        private List<String> newFeatures;
        private Map<String, String> codeExamples;
    }
}