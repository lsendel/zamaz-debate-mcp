package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Custom span processor that adds common attributes to all spans
 */
public class CustomSpanProcessor implements SpanProcessor {
    
    private static final AttributeKey<String> USER_ID = AttributeKey.stringKey("user.id");
    private static final AttributeKey<String> ORGANIZATION_ID = AttributeKey.stringKey("organization.id");
    private static final AttributeKey<String> REQUEST_ID = AttributeKey.stringKey("request.id");
    private static final AttributeKey<String> CLIENT_IP = AttributeKey.stringKey("client.ip");
    
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // Add user context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            span.setAttribute(USER_ID, authentication.getName());
        }
        
        // Add request context
        ServletRequestAttributes requestAttributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            
            // Add organization ID
            String orgId = request.getHeader("X-Organization-ID");
            if (orgId != null) {
                span.setAttribute(ORGANIZATION_ID, orgId);
            }
            
            // Add request ID
            String requestId = request.getHeader("X-Request-ID");
            if (requestId != null) {
                span.setAttribute(REQUEST_ID, requestId);
            } else {
                requestId = MDC.get("requestId");
                if (requestId != null) {
                    span.setAttribute(REQUEST_ID, requestId);
                }
            }
            
            // Add client IP
            String clientIp = request.getRemoteAddr();
            if (clientIp != null) {
                span.setAttribute(CLIENT_IP, clientIp);
            }
        }
    }
    
    @Override
    public boolean isStartRequired() {
        return true;
    }
    
    @Override
    public void onEnd(ReadableSpan span) {
        // No-op for now, but could be used for metrics collection
    }
    
    @Override
    public boolean isEndRequired() {
        return false;
    }
}