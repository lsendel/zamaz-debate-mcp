package com.zamaz.mcp.common.versioning;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor to handle API versioning concerns
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiVersionInterceptor implements HandlerInterceptor {

    private final ApiVersionResolver versionResolver;
    private final ApiVersionConfig config;
    private final ApiVersionMetrics metrics;
    
    private static final String VERSION_ATTRIBUTE = "api.version";
    private static final String VERSION_INFO_ATTRIBUTE = "api.version.info";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // Resolve API version
            ApiVersionResolver.ApiVersionInfo versionInfo = versionResolver.resolveVersion(request);
            
            // Store version info in request attributes
            request.setAttribute(VERSION_ATTRIBUTE, versionInfo.getVersion());
            request.setAttribute(VERSION_INFO_ATTRIBUTE, versionInfo);
            
            // Add version to tracing
            Span currentSpan = Span.current();
            if (currentSpan != null) {
                currentSpan.setAttribute("api.version", versionInfo.getVersion());
                currentSpan.setAttribute("api.version.source", versionInfo.getSource().toString());
                currentSpan.setAttribute("api.version.deprecated", versionInfo.isDeprecated());
            }
            
            // Record metrics
            if (config.isEnableVersionMetrics()) {
                metrics.recordVersionUsage(versionInfo.getVersion(), versionInfo.getSource());
            }
            
            // Add version to response headers
            if (config.isIncludeVersionInResponse()) {
                response.setHeader("X-API-Version", versionInfo.getVersion());
                response.setHeader("X-API-Version-Source", versionInfo.getSource().toString());
            }
            
            // Add deprecation warning if needed
            if (versionInfo.isDeprecated() && config.isIncludeDeprecationWarnings()) {
                response.setHeader("X-API-Deprecated", "true");
                response.setHeader("X-API-Deprecation-Warning", 
                    String.format("API version %s is deprecated. Please upgrade to version %s.", 
                        versionInfo.getVersion(), config.getLatestVersion()));
                
                log.warn("Deprecated API version {} used from {}", 
                    versionInfo.getVersion(), request.getRemoteAddr());
            }
            
            return true;
            
        } catch (ApiVersionResolver.UnsupportedApiVersionException e) {
            log.error("Unsupported API version requested: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"unsupported_version\", \"message\": \"%s\"}", 
                e.getMessage()));
            return false;
            
        } catch (ApiVersionResolver.MissingApiVersionException e) {
            log.error("Missing API version: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"missing_version\", \"message\": \"%s\"}", 
                e.getMessage()));
            return false;
            
        } catch (Exception e) {
            log.error("Error processing API version", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"version_processing_error\", \"message\": \"Internal server error\"}");
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Additional post-processing if needed
        ApiVersionResolver.ApiVersionInfo versionInfo = 
            (ApiVersionResolver.ApiVersionInfo) request.getAttribute(VERSION_INFO_ATTRIBUTE);
        
        if (versionInfo != null && config.isEnableVersionMetrics()) {
            metrics.recordResponseTime(versionInfo.getVersion(), 
                System.currentTimeMillis() - (Long) request.getAttribute("startTime"));
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Clean up if needed
        if (ex != null) {
            String version = (String) request.getAttribute(VERSION_ATTRIBUTE);
            if (version != null && config.isEnableVersionMetrics()) {
                metrics.recordError(version, ex);
            }
        }
    }
}