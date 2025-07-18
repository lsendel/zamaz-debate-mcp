package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.error.McpErrorCode;
import com.zamaz.mcp.common.error.McpErrorHandler;
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
import java.util.Map;
import java.util.UUID;

/**
 * AOP Aspect for handling MCP smart rate limiting annotations.
 * Provides intelligent rate limiting based on operation types and multi-tenant context.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class McpRateLimitAspect {

    private final McpRateLimitingService rateLimitingService;
    private final McpErrorHandler mcpErrorHandler;

    /**
     * Around advice for methods annotated with @McpRateLimit.
     */
    @Around("@annotation(com.zamaz.mcp.common.resilience.McpRateLimit)")
    public Object applySmartRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        McpRateLimit rateLimitAnnotation = method.getAnnotation(McpRateLimit.class);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Extract service and tool names
        String serviceName = extractServiceName(method);
        String toolName = extractToolName(method);
        
        // Check rate limit
        boolean permitted = rateLimitingService.isRequestPermitted(serviceName, toolName, authentication);
        
        if (!permitted) {
            log.warn("Rate limit exceeded for tool '{}' in service '{}' for user '{}'", 
                    toolName, serviceName, authentication != null ? authentication.getName() : "anonymous");
            
            return handleRateLimitExceeded(rateLimitAnnotation, method, serviceName, toolName);
        }

        try {
            log.debug("Rate limit check passed for tool '{}' in service '{}'", toolName, serviceName);
            return joinPoint.proceed();
            
        } catch (Throwable throwable) {
            // Log the error but don't count it against rate limits
            log.debug("Method execution failed after rate limit check: {}", throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Extract service name from method context.
     */
    private String extractServiceName(Method method) {
        String className = method.getDeclaringClass().getSimpleName();
        
        // Extract service name from class name patterns
        if (className.contains("Organization")) {
            return "organization";
        } else if (className.contains("Context")) {
            return "context";
        } else if (className.contains("Llm") || className.contains("LLM")) {
            return "llm";
        } else if (className.contains("Controller") || className.contains("Debate")) {
            return "controller";
        } else if (className.contains("Rag") || className.contains("RAG")) {
            return "rag";
        } else if (className.contains("Template")) {
            return "template";
        } else if (className.contains("Gateway")) {
            return "gateway";
        }
        
        // Fallback to package-based extraction
        String packageName = method.getDeclaringClass().getPackageName();
        if (packageName.contains("organization")) {
            return "organization";
        } else if (packageName.contains("context")) {
            return "context";
        } else if (packageName.contains("llm")) {
            return "llm";
        } else if (packageName.contains("controller")) {
            return "controller";
        } else if (packageName.contains("rag")) {
            return "rag";
        }
        
        return "unknown";
    }

    /**
     * Extract tool name from method name.
     */
    private String extractToolName(Method method) {
        String methodName = method.getName();
        
        // Convert method names to MCP tool names
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
        } else if (methodName.startsWith("search")) {
            return "search_" + camelToSnake(methodName.substring(6));
        } else if (methodName.startsWith("append")) {
            return "append_" + camelToSnake(methodName.substring(6));
        } else if (methodName.startsWith("generate")) {
            return "generate_" + camelToSnake(methodName.substring(8));
        } else if (methodName.startsWith("share")) {
            return "share_" + camelToSnake(methodName.substring(5));
        } else if (methodName.startsWith("add")) {
            return "add_" + camelToSnake(methodName.substring(3));
        } else if (methodName.startsWith("remove")) {
            return "remove_" + camelToSnake(methodName.substring(6));
        }
        
        return camelToSnake(methodName);
    }

    /**
     * Convert camelCase to snake_case.
     */
    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Handle rate limit exceeded scenario.
     */
    private Object handleRateLimitExceeded(McpRateLimit annotation, Method method, 
                                         String serviceName, String toolName) {
        
        // Try fallback method if specified
        if (!annotation.fallbackMethod().isEmpty()) {
            try {
                Object target = method.getDeclaringClass().getDeclaredConstructor().newInstance();
                Method fallbackMethod = method.getDeclaringClass()
                    .getDeclaredMethod(annotation.fallbackMethod(), method.getParameterTypes());
                
                log.debug("Executing fallback method '{}' for rate limited tool '{}'", 
                         annotation.fallbackMethod(), toolName);
                
                return fallbackMethod.invoke(target);
                
            } catch (Exception fallbackException) {
                log.warn("Fallback method '{}' failed: {}", annotation.fallbackMethod(), 
                        fallbackException.getMessage());
            }
        }

        // Create rate limit exception with context
        String errorMessage = !annotation.rateLimitMessage().isEmpty() 
            ? annotation.rateLimitMessage()
            : String.format("Rate limit exceeded for %s operation", toolName);
            
        McpRateLimitException rateLimitException = new McpRateLimitException(
            errorMessage, 
            serviceName, 
            toolName,
            new RequestNotPermitted("Rate limit exceeded")
        );

        // Return appropriate error response based on method return type
        if (method.getReturnType().equals(ResponseEntity.class)) {
            return mcpErrorHandler.createErrorResponse(
                rateLimitException,
                toolName,
                UUID.randomUUID().toString()
            );
        }

        // For other return types, throw exception
        throw rateLimitException;
    }

    /**
     * Custom exception for MCP rate limit scenarios.
     */
    public static class McpRateLimitException extends RuntimeException {
        private final String serviceName;
        private final String toolName;
        private final RequestNotPermitted rateLimitCause;

        public McpRateLimitException(String message, String serviceName, String toolName, RequestNotPermitted cause) {
            super(message, cause);
            this.serviceName = serviceName;
            this.toolName = toolName;
            this.rateLimitCause = cause;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getToolName() {
            return toolName;
        }

        public RequestNotPermitted getRateLimitCause() {
            return rateLimitCause;
        }

        /**
         * Get detailed error message with context.
         */
        public String getDetailedMessage() {
            return String.format("Rate limit exceeded for tool '%s' in service '%s': %s", 
                                toolName, serviceName, getMessage());
        }
    }
}