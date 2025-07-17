package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aspect that applies retry pattern to annotated methods.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Execute after circuit breaker
public class RetryAspect {

    private final RetryRegistry retryRegistry;

    @Around("@annotation(retryAnnotation)")
    public Object applyRetry(ProceedingJoinPoint joinPoint, Retry retryAnnotation) throws Throwable {
        String retryName = retryAnnotation.name();
        if (retryName.isEmpty()) {
            retryName = joinPoint.getSignature().toShortString();
        }

        // Get or create retry configuration
        io.github.resilience4j.retry.Retry retry;
        if (retryAnnotation.maxAttempts() == 3 && retryAnnotation.waitDurationMs() == 1000) {
            // Use default configuration
            retry = retryRegistry.retry(retryName);
        } else {
            // Create custom configuration
            io.github.resilience4j.retry.RetryConfig config = buildConfig(retryAnnotation);
            retry = retryRegistry.retry(retryName, config);
        }

        // Add tracing attributes
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("retry.name", retryName);
            currentSpan.setAttribute("retry.max_attempts", retryAnnotation.maxAttempts());
        }

        AtomicInteger attemptCount = new AtomicInteger(0);

        // Register event listeners for logging
        retry.getEventPublisher()
            .onRetry(event -> {
                int attempt = attemptCount.incrementAndGet();
                log.warn("Retry attempt {} for {}: {}", 
                    attempt, retryName, event.getLastThrowable().getMessage());
                
                if (currentSpan != null) {
                    currentSpan.setAttribute("retry.attempt", attempt);
                    currentSpan.addEvent("retry_attempt", 
                        io.opentelemetry.api.common.Attributes.builder()
                            .put("attempt", attempt)
                            .put("error", event.getLastThrowable().getMessage())
                            .build());
                }
            })
            .onSuccess(event -> {
                if (attemptCount.get() > 0) {
                    log.info("Retry succeeded for {} after {} attempts", 
                        retryName, attemptCount.get());
                }
                
                if (currentSpan != null) {
                    currentSpan.setAttribute("retry.succeeded", true);
                    currentSpan.setAttribute("retry.total_attempts", attemptCount.get() + 1);
                }
            })
            .onError(event -> {
                log.error("Retry failed for {} after {} attempts: {}", 
                    retryName, attemptCount.get(), event.getLastThrowable().getMessage());
                
                if (currentSpan != null) {
                    currentSpan.setAttribute("retry.succeeded", false);
                    currentSpan.setAttribute("retry.total_attempts", attemptCount.get());
                    currentSpan.recordException(event.getLastThrowable());
                }
            });

        // Execute with retry
        return retry.executeCallable(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /**
     * Build retry configuration from annotation
     */
    private io.github.resilience4j.retry.RetryConfig buildConfig(Retry annotation) {
        io.github.resilience4j.retry.RetryConfig.Builder<Object> builder = 
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(annotation.maxAttempts());

        // Configure wait duration
        if (annotation.exponentialBackoffMultiplier() > 1.0) {
            // Use exponential backoff
            if (annotation.useJitter()) {
                builder.intervalFunction(io.github.resilience4j.retry.IntervalFunction
                    .ofExponentialRandomBackoff(
                        annotation.waitDurationMs(),
                        annotation.exponentialBackoffMultiplier(),
                        annotation.jitterFactor(),
                        annotation.maxWaitDurationMs()
                    ));
            } else {
                builder.intervalFunction(io.github.resilience4j.retry.IntervalFunction
                    .ofExponentialBackoff(
                        annotation.waitDurationMs(),
                        annotation.exponentialBackoffMultiplier(),
                        annotation.maxWaitDurationMs()
                    ));
            }
        } else {
            // Use fixed wait duration
            builder.waitDuration(Duration.ofMillis(annotation.waitDurationMs()));
        }

        // Configure retry exceptions
        if (annotation.retryExceptions().length > 0) {
            builder.retryOnException(throwable -> {
                Class<?> throwableClass = throwable.getClass();
                return Arrays.stream(annotation.retryExceptions())
                    .anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(throwableClass));
            });
        }

        // Configure abort exceptions
        if (annotation.abortExceptions().length > 0) {
            builder.ignoreExceptions(annotation.abortExceptions());
        }

        return builder.build();
    }
}