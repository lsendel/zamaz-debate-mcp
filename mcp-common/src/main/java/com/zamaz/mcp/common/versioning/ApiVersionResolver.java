package com.zamaz.mcp.common.versioning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves API version from HTTP requests using configured strategy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiVersionResolver {

    private final ApiVersionConfig config;
    
    // Pattern for path-based versioning (e.g., /api/v1/users)
    private static final Pattern PATH_VERSION_PATTERN = Pattern.compile("/api/v(\\d+(?:\\.\\d+)*)/.*");
    
    // Pattern for content-type versioning (e.g., application/vnd.mcp.v1+json)
    private static final Pattern CONTENT_TYPE_VERSION_PATTERN = Pattern.compile("application/vnd\\.mcp\\.v(\\d+(?:\\.\\d+)*)\\+json");

    /**
     * Resolve API version from HTTP request
     */
    public ApiVersionInfo resolveVersion(HttpServletRequest request) {
        String version = null;
        VersionSource source = VersionSource.DEFAULT;
        
        switch (config.getStrategy()) {
            case HEADER:
                version = resolveFromHeader(request);
                source = VersionSource.HEADER;
                break;
            case PATH:
                version = resolveFromPath(request);
                source = VersionSource.PATH;
                break;
            case QUERY_PARAMETER:
                version = resolveFromQueryParameter(request);
                source = VersionSource.QUERY_PARAMETER;
                break;
            case CONTENT_TYPE:
                version = resolveFromContentType(request);
                source = VersionSource.CONTENT_TYPE;
                break;
            case MULTIPLE:
                ApiVersionInfo resolved = resolveFromMultipleSources(request);
                version = resolved.getVersion();
                source = resolved.getSource();
                break;
            case CUSTOM:
                version = resolveCustom(request);
                source = VersionSource.CUSTOM;
                break;
        }
        
        // Apply default behavior if no version found
        if (!StringUtils.hasText(version)) {
            version = applyDefaultBehavior(request);
            source = VersionSource.DEFAULT;
        }
        
        // Validate version
        if (config.isEnforceVersionValidation() && !config.isSupportedVersion(version)) {
            throw new UnsupportedApiVersionException(
                String.format("API version '%s' is not supported. Supported versions: %s", 
                    version, config.getSupportedVersions()));
        }
        
        boolean deprecated = config.isDeprecatedVersion(version);
        
        log.debug("Resolved API version: {} from source: {}, deprecated: {}", 
            version, source, deprecated);
        
        return ApiVersionInfo.builder()
            .version(version)
            .source(source)
            .deprecated(deprecated)
            .build();
    }
    
    /**
     * Resolve version from header
     */
    private String resolveFromHeader(HttpServletRequest request) {
        return request.getHeader(config.getVersionHeader());
    }
    
    /**
     * Resolve version from URL path
     */
    private String resolveFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        Matcher matcher = PATH_VERSION_PATTERN.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Resolve version from query parameter
     */
    private String resolveFromQueryParameter(HttpServletRequest request) {
        return request.getParameter(config.getVersionParameter());
    }
    
    /**
     * Resolve version from content type
     */
    private String resolveFromContentType(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        if (StringUtils.hasText(acceptHeader)) {
            Matcher matcher = CONTENT_TYPE_VERSION_PATTERN.matcher(acceptHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
    
    /**
     * Resolve version from multiple sources (priority order)
     */
    private ApiVersionInfo resolveFromMultipleSources(HttpServletRequest request) {
        // Priority: 1. Header, 2. Path, 3. Query Parameter, 4. Content Type
        
        String version = resolveFromHeader(request);
        if (StringUtils.hasText(version)) {
            return ApiVersionInfo.builder()
                .version(version)
                .source(VersionSource.HEADER)
                .deprecated(config.isDeprecatedVersion(version))
                .build();
        }
        
        version = resolveFromPath(request);
        if (StringUtils.hasText(version)) {
            return ApiVersionInfo.builder()
                .version(version)
                .source(VersionSource.PATH)
                .deprecated(config.isDeprecatedVersion(version))
                .build();
        }
        
        version = resolveFromQueryParameter(request);
        if (StringUtils.hasText(version)) {
            return ApiVersionInfo.builder()
                .version(version)
                .source(VersionSource.QUERY_PARAMETER)
                .deprecated(config.isDeprecatedVersion(version))
                .build();
        }
        
        version = resolveFromContentType(request);
        if (StringUtils.hasText(version)) {
            return ApiVersionInfo.builder()
                .version(version)
                .source(VersionSource.CONTENT_TYPE)
                .deprecated(config.isDeprecatedVersion(version))
                .build();
        }
        
        return ApiVersionInfo.builder()
            .version(null)
            .source(VersionSource.DEFAULT)
            .deprecated(false)
            .build();
    }
    
    /**
     * Custom version resolution logic
     */
    private String resolveCustom(HttpServletRequest request) {
        // Implement custom logic here
        // For example, based on client IP, user agent, or other factors
        return null;
    }
    
    /**
     * Apply default version behavior
     */
    private String applyDefaultBehavior(HttpServletRequest request) {
        switch (config.getDefaultVersionBehavior()) {
            case USE_DEFAULT:
                return config.getDefaultVersion();
            case USE_LATEST:
                return config.getLatestVersion();
            case REJECT:
                throw new MissingApiVersionException(
                    "API version is required but not specified");
            case REDIRECT:
                // Note: Redirect logic would be handled at the controller level
                return config.getDefaultVersion();
            default:
                return config.getDefaultVersion();
        }
    }
    
    /**
     * Version source enumeration
     */
    public enum VersionSource {
        HEADER,
        PATH,
        QUERY_PARAMETER,
        CONTENT_TYPE,
        CUSTOM,
        DEFAULT
    }
    
    /**
     * API version information
     */
    @lombok.Data
    @lombok.Builder
    public static class ApiVersionInfo {
        private String version;
        private VersionSource source;
        private boolean deprecated;
    }
    
    /**
     * Exception for unsupported API versions
     */
    public static class UnsupportedApiVersionException extends RuntimeException {
        public UnsupportedApiVersionException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception for missing API versions
     */
    public static class MissingApiVersionException extends RuntimeException {
        public MissingApiVersionException(String message) {
            super(message);
        }
    }
}