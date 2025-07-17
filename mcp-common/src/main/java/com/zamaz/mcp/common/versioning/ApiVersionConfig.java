package com.zamaz.mcp.common.versioning;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for API versioning strategy
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.api.versioning")
@Data
@Slf4j
public class ApiVersionConfig {

    /**
     * Default API version
     */
    private String defaultVersion = "1";
    
    /**
     * Current API version
     */
    private String currentVersion = "1";
    
    /**
     * Supported API versions
     */
    private List<String> supportedVersions = new ArrayList<>(List.of("1"));
    
    /**
     * Deprecated API versions
     */
    private List<String> deprecatedVersions = new ArrayList<>();
    
    /**
     * Versioning strategy
     */
    private VersioningStrategy strategy = VersioningStrategy.HEADER;
    
    /**
     * Version header name
     */
    private String versionHeader = "X-API-Version";
    
    /**
     * Version parameter name (for query/path parameter strategies)
     */
    private String versionParameter = "version";
    
    /**
     * URL prefix for path-based versioning
     */
    private String pathPrefix = "/api/v";
    
    /**
     * Media type template for content negotiation
     */
    private String mediaTypeTemplate = "application/vnd.mcp.v{version}+json";
    
    /**
     * Whether to include version in response headers
     */
    private boolean includeVersionInResponse = true;
    
    /**
     * Whether to include deprecation warnings in response
     */
    private boolean includeDeprecationWarnings = true;
    
    /**
     * Whether to enforce version validation
     */
    private boolean enforceVersionValidation = true;
    
    /**
     * Whether to log version usage statistics
     */
    private boolean enableVersionMetrics = true;
    
    /**
     * Default response when no version is specified
     */
    private DefaultVersionBehavior defaultVersionBehavior = DefaultVersionBehavior.USE_DEFAULT;
    
    /**
     * Versioning strategies
     */
    public enum VersioningStrategy {
        HEADER,           // X-API-Version header
        PATH,             // /api/v1/resource
        QUERY_PARAMETER,  // ?version=1
        CONTENT_TYPE,     // Accept: application/vnd.mcp.v1+json
        CUSTOM,           // Custom resolver
        MULTIPLE          // Support multiple strategies
    }
    
    /**
     * Default version behavior
     */
    public enum DefaultVersionBehavior {
        USE_DEFAULT,    // Use configured default version
        USE_LATEST,     // Use latest supported version
        REJECT,         // Reject requests without version
        REDIRECT        // Redirect to versioned endpoint
    }
    
    /**
     * Check if version is supported
     */
    public boolean isSupportedVersion(String version) {
        return supportedVersions.contains(version);
    }
    
    /**
     * Check if version is deprecated
     */
    public boolean isDeprecatedVersion(String version) {
        return deprecatedVersions.contains(version);
    }
    
    /**
     * Get the latest supported version
     */
    public String getLatestVersion() {
        return supportedVersions.isEmpty() ? defaultVersion : 
            supportedVersions.get(supportedVersions.size() - 1);
    }
    
    /**
     * Add a new supported version
     */
    public void addSupportedVersion(String version) {
        if (!supportedVersions.contains(version)) {
            supportedVersions.add(version);
            log.info("Added supported API version: {}", version);
        }
    }
    
    /**
     * Mark version as deprecated
     */
    public void deprecateVersion(String version) {
        if (supportedVersions.contains(version) && !deprecatedVersions.contains(version)) {
            deprecatedVersions.add(version);
            log.warn("Marked API version {} as deprecated", version);
        }
    }
    
    /**
     * Remove support for a version
     */
    public void removeSupportedVersion(String version) {
        supportedVersions.remove(version);
        deprecatedVersions.remove(version);
        log.warn("Removed support for API version: {}", version);
    }
}