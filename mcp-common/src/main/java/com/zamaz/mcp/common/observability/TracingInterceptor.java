package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Spring MVC interceptor that enhances tracing with additional context and baggage.
 */
@Component
@Slf4j
public class TracingInterceptor implements HandlerInterceptor {
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String ORGANIZATION_ID_HEADER = "X-Organization-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String SESSION_ID_HEADER = "X-Session-ID";
    
    private final Tracer tracer;
    
    @Autowired
    public TracingInterceptor(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(TracingInterceptor.class.getName(), "1.0.0");
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Get or create request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            response.setHeader(REQUEST_ID_HEADER, requestId);
        }
        
        // Get current span
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            // Add request attributes to span
            currentSpan.setAttribute("http.request_id", requestId);
            currentSpan.setAttribute("http.method", request.getMethod());
            currentSpan.setAttribute("http.url", request.getRequestURL().toString());
            currentSpan.setAttribute("http.user_agent", request.getHeader("User-Agent"));
            
            // Add organization context if present
            String organizationId = request.getHeader(ORGANIZATION_ID_HEADER);
            if (organizationId != null) {
                currentSpan.setAttribute("organization.id", organizationId);
            }
            
            // Add user context if present
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId != null) {
                currentSpan.setAttribute("user.id", userId);
            }
            
            // Add session context if present
            String sessionId = request.getHeader(SESSION_ID_HEADER);
            if (sessionId != null) {
                currentSpan.setAttribute("session.id", sessionId);
            }
        }
        
        // Set up baggage for propagation
        Baggage.Builder baggageBuilder = Baggage.builder();
        baggageBuilder.put("request.id", requestId);
        
        if (request.getHeader(ORGANIZATION_ID_HEADER) != null) {
            baggageBuilder.put("organization.id", request.getHeader(ORGANIZATION_ID_HEADER));
        }
        
        if (request.getHeader(USER_ID_HEADER) != null) {
            baggageBuilder.put("user.id", request.getHeader(USER_ID_HEADER));
        }
        
        if (request.getHeader(SESSION_ID_HEADER) != null) {
            baggageBuilder.put("session.id", request.getHeader(SESSION_ID_HEADER));
        }
        
        Baggage baggage = baggageBuilder.build();
        
        // Store the baggage in request attributes for later use
        request.setAttribute("otel.baggage", baggage);
        
        // Make baggage current for the request
        try (Scope ignored = baggage.makeCurrent()) {
            // The baggage is now available in the current context
            log.debug("Baggage set for request: {}", requestId);
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                          ModelAndView modelAndView) throws Exception {
        // Add response headers for trace correlation
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            SpanContext spanContext = currentSpan.getSpanContext();
            response.setHeader("X-Trace-ID", spanContext.getTraceId());
            response.setHeader("X-Span-ID", spanContext.getSpanId());
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // Add response status to span
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            currentSpan.setAttribute("http.status_code", response.getStatus());
            
            // Record exception if present
            if (ex != null) {
                currentSpan.recordException(ex);
            }
        }
        
        // Log trace information for debugging
        if (log.isDebugEnabled()) {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null) {
                requestId = response.getHeader(REQUEST_ID_HEADER);
            }
            log.debug("Request completed - ID: {}, Status: {}, Method: {} {}", 
                     requestId, response.getStatus(), request.getMethod(), request.getRequestURI());
        }
    }
}