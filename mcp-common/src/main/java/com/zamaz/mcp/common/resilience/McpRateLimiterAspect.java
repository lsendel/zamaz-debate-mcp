package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.error.McpErrorCode;
import com.zamaz.mcp.common.error.McpErrorHandler;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * AOP Aspect for handling MCP rate limiting annotations.
 * Applies rate limiting to MCP tool endpoints with organization and user-based limits.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class McpRateLimiterAspect {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final McpErrorHandler mcpErrorHandler;

    /**
     * Around advice for methods annotated with @RateLimiter.
     */
    @Around("@annotation(com.zamaz.mcp.common.resilience.RateLimiter)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimiter rateLimiterAnnotation = method.getAnnotation(RateLimiter.class);

        // Create rate limiter name based on context
        String rateLimiterName = createRateLimiterName(rateLimiterAnnotation, method);
        
        // Get or create rate limiter with custom configuration
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getRateLimiter(
            rateLimiterName, 
            rateLimiterAnnotation
        );

        try {
            log.debug("Applying rate limit '{}' to method '{}'", rateLimiterName, method.getName());
            
            // Execute method with rate limiting
            return rateLimiter.executeSupplier(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
            
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for '{}' on method '{}'", rateLimiterName, method.getName());
            
            // Handle rate limit exceeded
            return handleRateLimitExceeded(rateLimiterAnnotation, method, e);
            
        } catch (Exception e) {
            if (e.getCause() instanceof Throwable) {
                throw (Throwable) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Create a unique rate limiter name based on context.
     */
    private String createRateLimiterName(RateLimiter annotation, Method method) {
        String baseName = annotation.name();
        if (baseName.isEmpty()) {
            baseName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }

        // Add organization and user context for multi-tenant rate limiting
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String userId = authentication.getName();
            String organizationId = extractOrganizationId(authentication);
            
            if (organizationId != null) {
                return String.format("%s:org:%s:user:%s", baseName, organizationId, userId);
            } else {
                return String.format("%s:user:%s", baseName, userId);
            }
        }

        // Fallback to base name if no authentication context
        return baseName + ":anonymous";
    }

    /**
     * Extract organization ID from authentication context.
     */
    private String extractOrganizationId(Authentication authentication) {
        if (authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("ORG_"))
                .findFirst()
                .map(orgAuth -> orgAuth.getAuthority().substring(4)) // Remove "ORG_" prefix
                .orElse(null);
        }
        return null;
    }

    /**
     * Get or create rate limiter with custom configuration.
     */
    private io.github.resilience4j.ratelimiter.RateLimiter getRateLimiter(
            String name, 
            RateLimiter annotation) {
        
        // Create custom configuration based on annotation
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(annotation.limitForPeriod())
                .limitRefreshPeriod(Duration.ofSeconds(annotation.limitRefreshPeriodSeconds()))
                .timeoutDuration(Duration.ofSeconds(annotation.timeoutDurationSeconds()))
                .build();

        // Get or create rate limiter with the configuration
        return rateLimiterRegistry.rateLimiter(name, config);
    }

    /**
     * Handle rate limit exceeded scenario.
     */
    private Object handleRateLimitExceeded(RateLimiter annotation, Method method, RequestNotPermitted e) {
        // Try fallback method if specified
        if (!annotation.fallbackMethod().isEmpty()) {
            try {
                Method fallbackMethod = method.getDeclaringClass()
                    .getDeclaredMethod(annotation.fallbackMethod(), method.getParameterTypes());
                
                log.debug("Executing fallback method '{}' for rate limited method '{}'", 
                         annotation.fallbackMethod(), method.getName());
                
                // This is a simplified fallback - in practice, you might need to handle parameters
                return fallbackMethod.invoke(method.getDeclaringClass().newInstance());
                
            } catch (Exception fallbackException) {
                log.warn("Fallback method '{}' failed: {}", annotation.fallbackMethod(), 
                        fallbackException.getMessage());
            }
        }

        // Return appropriate error response based on method return type
        if (method.getReturnType().equals(ResponseEntity.class)) {
            return mcpErrorHandler.createErrorResponse(
                new McpRateLimitException("Rate limit exceeded", e),
                extractToolName(method),
                UUID.randomUUID().toString()
            );
        }

        // For other return types, throw exception
        throw new McpRateLimitException("Rate limit exceeded for " + method.getName(), e);
    }

    /**
     * Extract tool name from method for error reporting.
     */
    private String extractToolName(Method method) {
        String methodName = method.getName();
        
        // Convert method names to tool names (e.g., createOrganization -> create_organization)
        if (methodName.startsWith("create")) {
            return "create_" + camelToSnake(methodName.substring(6));
        } else if (methodName.startsWith("get")) {
            return "get_" + camelToSnake(methodName.substring(3));
        } else if (methodName.startsWith("update")) {
            return "update_" + camelToSnake(methodName.substring(6));
        } else if (methodName.startsWith("delete")) {
            return "delete_" + camelToSnake(methodName.substring(6));
        } else if (methodName.startsWith("list")) {
            return "list_" + camelToSnake(methodName.substring(4));
        }
        
        return camelToSnake(methodName);
    }

    /**
     * Convert camelCase to snake_case.
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Custom exception for rate limit exceeded scenarios.
     */
    public static class McpRateLimitException extends RuntimeException {
        private final RequestNotPermitted cause;

        public McpRateLimitException(String message, RequestNotPermitted cause) {
            super(message, cause);
            this.cause = cause;
        }

        public RequestNotPermitted getRateLimitCause() {
            return cause;
        }
    }
}