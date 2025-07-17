package com.zamaz.mcp.common.versioning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for API versioning operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiVersionUtils {

    private final ApiVersionConfig config;

    /**
     * Get current API version from request context
     */
    public String getCurrentVersion() {
        ServletRequestAttributes requestAttributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            return (String) request.getAttribute("api.version");
        }
        
        return config.getDefaultVersion();
    }

    /**
     * Get current API version info from request context
     */
    public ApiVersionResolver.ApiVersionInfo getCurrentVersionInfo() {
        ServletRequestAttributes requestAttributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            return (ApiVersionResolver.ApiVersionInfo) request.getAttribute("api.version.info");
        }
        
        return null;
    }

    /**
     * Check if current version is deprecated
     */
    public boolean isCurrentVersionDeprecated() {
        ApiVersionResolver.ApiVersionInfo versionInfo = getCurrentVersionInfo();
        return versionInfo != null && versionInfo.isDeprecated();
    }

    /**
     * Get URL for specific version
     */
    public String getVersionedUrl(String baseUrl, String version) {
        switch (config.getStrategy()) {
            case PATH:
                return baseUrl.replace("/api/", "/api/v" + version + "/");
            case QUERY_PARAMETER:
                String separator = baseUrl.contains("?") ? "&" : "?";
                return baseUrl + separator + config.getVersionParameter() + "=" + version;
            default:
                return baseUrl;
        }
    }

    /**
     * Get supported versions for API documentation
     */
    public List<VersionInfo> getSupportedVersionsInfo() {
        return config.getSupportedVersions().stream()
            .map(version -> VersionInfo.builder()
                .version(version)
                .deprecated(config.isDeprecatedVersion(version))
                .current(version.equals(config.getCurrentVersion()))
                .latest(version.equals(config.getLatestVersion()))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Compare two version strings
     */
    public int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) return 0;
        if (version1 == null) return -1;
        if (version2 == null) return 1;

        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Part < v2Part) return -1;
            if (v1Part > v2Part) return 1;
        }

        return 0;
    }

    /**
     * Check if version is newer than another
     */
    public boolean isVersionNewer(String version1, String version2) {
        return compareVersions(version1, version2) > 0;
    }

    /**
     * Check if version is older than another
     */
    public boolean isVersionOlder(String version1, String version2) {
        return compareVersions(version1, version2) < 0;
    }

    /**
     * Get next version in sequence
     */
    public String getNextVersion(String currentVersion) {
        String[] parts = currentVersion.split("\\.");
        if (parts.length == 1) {
            return String.valueOf(Integer.parseInt(parts[0]) + 1);
        } else {
            int lastPart = Integer.parseInt(parts[parts.length - 1]) + 1;
            parts[parts.length - 1] = String.valueOf(lastPart);
            return String.join(".", parts);
        }
    }

    /**
     * Generate deprecation message for version
     */
    public String getDeprecationMessage(String version) {
        return String.format(
            "API version %s is deprecated and will be removed in the future. " +
            "Please upgrade to version %s. " +
            "For migration guide, see: /api/docs/migration/v%s-to-v%s",
            version, config.getLatestVersion(), version, config.getLatestVersion()
        );
    }

    /**
     * Version information for documentation
     */
    @lombok.Data
    @lombok.Builder
    public static class VersionInfo {
        private String version;
        private boolean deprecated;
        private boolean current;
        private boolean latest;
        private String deprecationMessage;
        private String migrationGuide;
    }
}