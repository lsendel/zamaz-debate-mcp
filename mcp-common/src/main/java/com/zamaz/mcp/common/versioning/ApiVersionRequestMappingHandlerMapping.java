package com.zamaz.mcp.common.versioning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom request mapping handler that supports API versioning
 */
@RequiredArgsConstructor
@Slf4j
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private final ApiVersionResolver versionResolver;
    private final ApiVersionConfig config;

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
        
        if (info == null) {
            return null;
        }
        
        // Check for API versioning annotation on method
        ApiVersioning methodVersioning = AnnotationUtils.findAnnotation(method, ApiVersioning.class);
        
        // Check for API versioning annotation on class
        ApiVersioning classVersioning = AnnotationUtils.findAnnotation(handlerType, ApiVersioning.class);
        
        // Combine class and method versioning
        ApiVersioning effectiveVersioning = combineVersioning(classVersioning, methodVersioning);
        
        if (effectiveVersioning != null) {
            ApiVersionCondition versionCondition = new ApiVersionCondition(effectiveVersioning);
            info = info.combine(RequestMappingInfo.paths().build().combine(
                RequestMappingInfo.paths().build().combine(
                    new RequestMappingInfo(null, null, null, null, null, null, versionCondition)
                )
            ));
        }
        
        return info;
    }

    /**
     * Combine class and method level versioning annotations
     */
    private ApiVersioning combineVersioning(ApiVersioning classVersioning, ApiVersioning methodVersioning) {
        if (methodVersioning != null) {
            return methodVersioning;
        }
        return classVersioning;
    }

    /**
     * Custom request condition for API versioning
     */
    private class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {
        
        private final Set<String> supportedVersions;
        private final String minVersion;
        private final String maxVersion;
        private final boolean deprecated;
        private final String deprecationMessage;
        private final String removedInVersion;
        
        public ApiVersionCondition(ApiVersioning versioning) {
            this.supportedVersions = new HashSet<>(Arrays.asList(versioning.value()));
            this.minVersion = versioning.min();
            this.maxVersion = versioning.max();
            this.deprecated = versioning.deprecated();
            this.deprecationMessage = versioning.deprecationMessage();
            this.removedInVersion = versioning.removedInVersion();
        }
        
        public ApiVersionCondition(Set<String> supportedVersions, String minVersion, String maxVersion, 
                                 boolean deprecated, String deprecationMessage, String removedInVersion) {
            this.supportedVersions = supportedVersions;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            this.deprecated = deprecated;
            this.deprecationMessage = deprecationMessage;
            this.removedInVersion = removedInVersion;
        }

        @Override
        public ApiVersionCondition combine(ApiVersionCondition other) {
            // Method-level versioning takes precedence over class-level
            Set<String> combinedVersions = new HashSet<>(supportedVersions);
            combinedVersions.addAll(other.supportedVersions);
            
            return new ApiVersionCondition(
                combinedVersions,
                other.minVersion.isEmpty() ? minVersion : other.minVersion,
                other.maxVersion.isEmpty() ? maxVersion : other.maxVersion,
                other.deprecated || deprecated,
                other.deprecationMessage.isEmpty() ? deprecationMessage : other.deprecationMessage,
                other.removedInVersion.isEmpty() ? removedInVersion : other.removedInVersion
            );
        }

        @Override
        public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
            try {
                ApiVersionResolver.ApiVersionInfo versionInfo = versionResolver.resolveVersion(request);
                String requestedVersion = versionInfo.getVersion();
                
                // Check if version is explicitly supported
                if (supportedVersions.contains(requestedVersion)) {
                    return this;
                }
                
                // Check version range if specified
                if (!minVersion.isEmpty() && !maxVersion.isEmpty()) {
                    if (isVersionInRange(requestedVersion, minVersion, maxVersion)) {
                        return this;
                    }
                }
                
                // Check if version is removed
                if (!removedInVersion.isEmpty() && 
                    isVersionGreaterOrEqual(requestedVersion, removedInVersion)) {
                    return null; // Endpoint is removed in this version
                }
                
                return null;
                
            } catch (Exception e) {
                log.error("Error matching version condition", e);
                return null;
            }
        }

        @Override
        public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
            // More specific version conditions should be preferred
            if (supportedVersions.size() != other.supportedVersions.size()) {
                return Integer.compare(supportedVersions.size(), other.supportedVersions.size());
            }
            
            // Prefer non-deprecated versions
            if (deprecated != other.deprecated) {
                return deprecated ? 1 : -1;
            }
            
            return 0;
        }

        /**
         * Check if version is in specified range
         */
        private boolean isVersionInRange(String version, String min, String max) {
            return isVersionGreaterOrEqual(version, min) && isVersionLessOrEqual(version, max);
        }

        /**
         * Compare version strings (basic implementation)
         */
        private boolean isVersionGreaterOrEqual(String version, String compareVersion) {
            if (version == null || compareVersion == null) {
                return false;
            }
            
            String[] versionParts = version.split("\\.");
            String[] compareParts = compareVersion.split("\\.");
            
            int maxLength = Math.max(versionParts.length, compareParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int versionPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
                int comparePart = i < compareParts.length ? Integer.parseInt(compareParts[i]) : 0;
                
                if (versionPart > comparePart) {
                    return true;
                } else if (versionPart < comparePart) {
                    return false;
                }
            }
            
            return true; // Equal
        }

        /**
         * Check if version is less than or equal to compare version
         */
        private boolean isVersionLessOrEqual(String version, String compareVersion) {
            if (version == null || compareVersion == null) {
                return false;
            }
            
            return version.equals(compareVersion) || !isVersionGreaterOrEqual(version, compareVersion);
        }
    }
}