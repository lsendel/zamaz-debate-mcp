package com.zamaz.mcp.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Filter for logging all HTTP requests and responses with structured data
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private final StructuredLogger structuredLogger;
    
    @Value("${logging.request.enabled:true}")
    private boolean requestLoggingEnabled;
    
    @Value("${logging.request.include-body:false}")
    private boolean includeRequestBody;
    
    @Value("${logging.request.include-response-body:false}")
    private boolean includeResponseBody;
    
    @Value("${logging.request.max-body-size:1000}")
    private int maxBodySize;
    
    // Sensitive headers to exclude from logging
    private static final List<String> SENSITIVE_HEADERS = Arrays.asList(
        "authorization", "cookie", "set-cookie", "x-auth-token", "x-api-key"
    );
    
    // Paths to exclude from detailed logging
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/actuator/health", "/actuator/metrics", "/actuator/prometheus",
        "/favicon.ico", "/robots.txt"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        if (!requestLoggingEnabled || shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Generate request ID if not present
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        
        // Set MDC for the entire request
        MDC.put("requestId", requestId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestUri", request.getRequestURI());
        
        try {
            // Wrap request and response for content caching
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            
            long startTime = System.currentTimeMillis();
            Instant requestTime = Instant.now();
            
            // Log request
            logRequest(wrappedRequest, requestId, requestTime);
            
            try {
                // Process the request
                filterChain.doFilter(wrappedRequest, wrappedResponse);
                
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                // Log response
                logResponse(wrappedRequest, wrappedResponse, requestId, duration, null);
                
                // Copy body to response
                wrappedResponse.copyBodyToResponse();
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - System.currentTimeMillis();
            logResponse(request, response, requestId, duration, e);
            throw e;
        } finally {
            // Clear MDC
            MDC.clear();
        }
    }
    
    /**
     * Log incoming request
     */
    private void logRequest(ContentCachingRequestWrapper request, String requestId, Instant requestTime) {
        LogContext context = LogContext.builder()
            .requestId(requestId)
            .operation("HTTP_REQUEST")
            .component("RequestFilter")
            .build()
            .addMetadata("httpMethod", request.getMethod())
            .addMetadata("requestUri", request.getRequestURI())
            .addMetadata("queryString", request.getQueryString())
            .addMetadata("protocol", request.getProtocol())
            .addMetadata("contentType", request.getContentType())
            .addMetadata("contentLength", request.getContentLengthLong())
            .addMetadata("remoteAddr", request.getRemoteAddr())
            .addMetadata("requestTime", requestTime.toString());
        
        // Add headers (excluding sensitive ones)
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            if (!SENSITIVE_HEADERS.contains(headerName.toLowerCase())) {
                context.addMetadata("header_" + headerName, request.getHeader(headerName));
            }
        });
        
        // Add request body if enabled and appropriate
        if (includeRequestBody && shouldIncludeBody(request)) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, 0, Math.min(content.length, maxBodySize));
                context.addMetadata("requestBody", body);
                if (content.length > maxBodySize) {
                    context.addMetadata("requestBodyTruncated", true);
                }
            }
        }
        
        structuredLogger.info("HTTP", "Incoming HTTP request", context);
    }
    
    /**
     * Log outgoing response
     */
    private void logResponse(HttpServletRequest request, HttpServletResponse response, 
                           String requestId, long duration, Exception exception) {
        
        LogContext context = LogContext.builder()
            .requestId(requestId)
            .operation("HTTP_RESPONSE")
            .component("RequestFilter")
            .duration(duration)
            .statusCode(response.getStatus())
            .exception(exception)
            .build()
            .addMetadata("httpMethod", request.getMethod())
            .addMetadata("requestUri", request.getRequestURI())
            .addMetadata("statusCode", response.getStatus())
            .addMetadata("contentType", response.getContentType())
            .addMetadata("duration", duration);
        
        // Add response headers (excluding sensitive ones)
        response.getHeaderNames().forEach(headerName -> {
            if (!SENSITIVE_HEADERS.contains(headerName.toLowerCase())) {
                context.addMetadata("responseHeader_" + headerName, response.getHeader(headerName));
            }
        });
        
        // Add response body if enabled and appropriate
        if (includeResponseBody && response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, 0, Math.min(content.length, maxBodySize));
                context.addMetadata("responseBody", body);
                if (content.length > maxBodySize) {
                    context.addMetadata("responseBodyTruncated", true);
                }
            }
        }
        
        // Performance categorization
        String performanceCategory = categorizePerformance(duration);
        context.addMetadata("performanceCategory", performanceCategory);
        
        // Security indicators
        if (response.getStatus() == 401) {
            context.addMetadata("securityEvent", "UNAUTHORIZED_ACCESS");
        } else if (response.getStatus() == 403) {
            context.addMetadata("securityEvent", "FORBIDDEN_ACCESS");
        } else if (response.getStatus() == 429) {
            context.addMetadata("securityEvent", "RATE_LIMITED");
        }
        
        String logLevel = determineLogLevel(response.getStatus(), duration, exception);
        String message = String.format("HTTP %s %s - %d (%dms)", 
            request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        
        switch (logLevel) {
            case "ERROR":
                structuredLogger.error("HTTP", message, context);
                break;
            case "WARN":
                structuredLogger.warn("HTTP", message, context);
                break;
            case "DEBUG":
                structuredLogger.debug("HTTP", message, context);
                break;
            default:
                structuredLogger.info("HTTP", message, context);
        }
    }
    
    /**
     * Check if request should be skipped from logging
     */
    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
    
    /**
     * Check if request body should be included
     */
    private boolean shouldIncludeBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && 
               (contentType.startsWith("application/json") || 
                contentType.startsWith("application/xml") ||
                contentType.startsWith("text/"));
    }
    
    /**
     * Categorize performance based on duration
     */
    private String categorizePerformance(long duration) {
        if (duration < 100) {
            return "EXCELLENT";
        } else if (duration < 500) {
            return "GOOD";
        } else if (duration < 1000) {
            return "ACCEPTABLE";
        } else if (duration < 5000) {
            return "SLOW";
        } else {
            return "VERY_SLOW";
        }
    }
    
    /**
     * Determine appropriate log level based on response
     */
    private String determineLogLevel(int statusCode, long duration, Exception exception) {
        if (exception != null || statusCode >= 500) {
            return "ERROR";
        } else if (statusCode >= 400 || duration > 5000) {
            return "WARN";
        } else if (duration > 1000) {
            return "INFO";
        } else {
            return "DEBUG";
        }
    }
}