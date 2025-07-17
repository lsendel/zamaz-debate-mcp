package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Aspect for tracing method executions.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TracingAspect {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    /**
     * Constructor that creates a tracer from OpenTelemetry.
     *
     * @param openTelemetry the OpenTelemetry instance
     */
    public TracingAspect(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer(TracingAspect.class.getName(), "1.0.0");
    }

    /**
     * Pointcut for service methods.
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceMethod() {
    }

    /**
     * Pointcut for repository methods.
     */
    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryMethod() {
    }

    /**
     * Pointcut for controller methods.
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerMethod() {
    }

    /**
     * Pointcut for methods annotated with Traced.
     */
    @Pointcut("@annotation(com.zamaz.mcp.common.observability.Traced)")
    public void tracedMethod() {
    }

    /**
     * Around advice for service methods.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if an error occurs
     */
    @Around("serviceMethod() || repositoryMethod() || controllerMethod() || tracedMethod()")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method signature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Create span name from class and method
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String spanName = className + "." + methodName;
        
        // Get current span or create a new one
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(getSpanKind(joinPoint))
                .startSpan();
        
        // Add method parameters as attributes (be careful with sensitive data)
        try {
            addMethodParametersToSpan(span, signature, joinPoint.getArgs());
        } catch (Exception e) {
            log.warn("Failed to add method parameters to span", e);
        }
        
        // Execute method with the span in context
        try (Scope scope = span.makeCurrent()) {
            return joinPoint.proceed();
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }
    
    /**
     * Get the span kind based on the join point.
     *
     * @param joinPoint the join point
     * @return the span kind
     */
    private SpanKind getSpanKind(ProceedingJoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        if (targetClass.getName().contains("Controller")) {
            return SpanKind.SERVER;
        } else if (targetClass.getName().contains("Repository")) {
            return SpanKind.CLIENT;
        } else {
            return SpanKind.INTERNAL;
        }
    }
    
    /**
     * Add method parameters to the span.
     *
     * @param span the span
     * @param signature the method signature
     * @param args the method arguments
     */
    private void addMethodParametersToSpan(Span span, MethodSignature signature, Object[] args) {
        String[] parameterNames = signature.getParameterNames();
        
        if (parameterNames != null && parameterNames.length > 0) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (i < args.length && args[i] != null) {
                    // Skip large objects and potentially sensitive data
                    if (shouldAddParameterToSpan(parameterNames[i], args[i])) {
                        String value = getParameterValue(args[i]);
                        span.setAttribute("param." + parameterNames[i], value);
                    }
                }
            }
        }
    }
    
    /**
     * Determine if a parameter should be added to the span.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @return true if the parameter should be added
     */
    private boolean shouldAddParameterToSpan(String name, Object value) {
        // Skip parameters with sensitive names
        if (name.toLowerCase().contains("password") || 
            name.toLowerCase().contains("secret") || 
            name.toLowerCase().contains("token") || 
            name.toLowerCase().contains("key")) {
            return false;
        }
        
        // Skip large objects
        if (value instanceof byte[] && ((byte[]) value).length > 1000) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get a string representation of a parameter value.
     *
     * @param value the parameter value
     * @return the string representation
     */
    private String getParameterValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value.getClass().isArray()) {
            if (value instanceof Object[]) {
                return Arrays.stream((Object[]) value)
                        .map(Object::toString)
                        .collect(Collectors.joining(", ", "[", "]"));
            } else {
                return "Array of " + value.getClass().getComponentType().getSimpleName();
            }
        }
        
        // Limit string length
        String stringValue = value.toString();
        if (stringValue.length() > 100) {
            return stringValue.substring(0, 97) + "...";
        }
        
        return stringValue;
    }
}
