package com.zamaz.mcp.common.versioning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for API versioning information and management
 */
@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
@Slf4j
public class ApiVersionController {

    private final ApiVersionConfig config;
    private final ApiVersionUtils versionUtils;
    private final ApiVersionDocumentationGenerator documentationGenerator;

    /**
     * Get API versioning information
     */
    @GetMapping
    public Map<String, Object> getVersionInfo() {
        return Map.of(
            "current", config.getCurrentVersion(),
            "default", config.getDefaultVersion(),
            "latest", config.getLatestVersion(),
            "supported", config.getSupportedVersions(),
            "deprecated", config.getDeprecatedVersions(),
            "strategy", config.getStrategy(),
            "client_version", versionUtils.getCurrentVersion(),
            "client_deprecated", versionUtils.isCurrentVersionDeprecated()
        );
    }

    /**
     * Get detailed API documentation
     */
    @GetMapping("/documentation")
    public ApiVersionDocumentationGenerator.ApiVersionDocumentation getDocumentation() {
        return documentationGenerator.generateDocumentation();
    }

    /**
     * Get supported versions with detailed information
     */
    @GetMapping("/supported")
    public Map<String, Object> getSupportedVersions() {
        return Map.of(
            "versions", versionUtils.getSupportedVersionsInfo(),
            "total", config.getSupportedVersions().size(),
            "deprecated_count", config.getDeprecatedVersions().size()
        );
    }

    /**
     * Example endpoint with version-specific behavior
     */
    @GetMapping("/example")
    @ApiVersioning({"1", "2"})
    public Map<String, Object> getExampleV1And2() {
        String version = versionUtils.getCurrentVersion();
        
        if ("1".equals(version)) {
            return Map.of(
                "message", "This is version 1 response",
                "data", Map.of("id", 1, "name", "John Doe"),
                "version", version
            );
        } else {
            return Map.of(
                "message", "This is version 2 response",
                "data", Map.of("identifier", 1, "full_name", "John Doe", "created_at", "2023-01-01"),
                "version", version,
                "new_field", "This field was added in v2"
            );
        }
    }

    /**
     * Example endpoint only available in specific versions
     */
    @GetMapping("/legacy")
    @ApiVersioning(value = {"1"}, deprecated = true, 
                  deprecationMessage = "This endpoint is deprecated, use /api/versions/example instead")
    public Map<String, Object> getLegacyEndpoint() {
        return Map.of(
            "message", "This is a legacy endpoint",
            "warning", "This endpoint is deprecated and will be removed in v2",
            "version", versionUtils.getCurrentVersion()
        );
    }

    /**
     * Example endpoint with version range
     */
    @GetMapping("/range")
    @ApiVersioning(min = "1", max = "2")
    public Map<String, Object> getVersionRange() {
        return Map.of(
            "message", "This endpoint supports versions 1 through 2",
            "version", versionUtils.getCurrentVersion(),
            "supported_range", "1-2"
        );
    }

    /**
     * Example endpoint only in latest version
     */
    @GetMapping("/latest")
    @ApiVersioning({"2"})
    public Map<String, Object> getLatestOnly() {
        return Map.of(
            "message", "This endpoint is only available in the latest version",
            "version", versionUtils.getCurrentVersion(),
            "new_features", Map.of(
                "feature1", "Enhanced performance",
                "feature2", "Better error handling"
            )
        );
    }
}