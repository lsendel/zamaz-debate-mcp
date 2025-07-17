package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for distributed tracing configuration
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DistributedTracingConfig.class})
@TestPropertySource(properties = {
    "spring.application.name=test-service",
    "mcp.tracing.enabled=true",
    "mcp.tracing.exporters.otlp.enabled=false",
    "mcp.tracing.exporters.jaeger.enabled=false",
    "mcp.tracing.exporters.logging.enabled=true",
    "mcp.tracing.sampling.strategy=always_on"
})
class DistributedTracingConfigTest {
    
    @Autowired
    private OpenTelemetry openTelemetry;
    
    @Autowired
    private Environment environment;
    
    @Test
    void testOpenTelemetryBeanCreation() {
        assertNotNull(openTelemetry);
        assertNotEquals(OpenTelemetry.noop(), openTelemetry);
    }
    
    @Test
    void testTracerCreation() {
        Tracer tracer = openTelemetry.getTracer("test-tracer", "1.0.0");
        assertNotNull(tracer);
        
        // Create a test span
        Span span = tracer.spanBuilder("test-span").startSpan();
        try (Scope scope = span.makeCurrent()) {
            assertTrue(span.getSpanContext().isValid());
            assertNotNull(span.getSpanContext().getTraceId());
            assertNotNull(span.getSpanContext().getSpanId());
        } finally {
            span.end();
        }
    }
    
    @Test
    void testRateLimitingSampler() {
        RateLimitingSampler sampler = new RateLimitingSampler(10);
        assertNotNull(sampler);
        assertEquals("RateLimitingSampler{maxTracesPerSecond=10}", sampler.getDescription());
    }
    
    @Test
    void testRateLimitingSamplerInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitingSampler(0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitingSampler(-1));
    }
    
    @Test
    void testNestedSpans() {
        Tracer tracer = openTelemetry.getTracer("test-tracer", "1.0.0");
        
        // Create parent span
        Span parentSpan = tracer.spanBuilder("parent-operation").startSpan();
        try (Scope parentScope = parentSpan.makeCurrent()) {
            
            // Create child span
            Span childSpan = tracer.spanBuilder("child-operation").startSpan();
            try (Scope childScope = childSpan.makeCurrent()) {
                
                // Verify parent-child relationship
                assertNotEquals(parentSpan.getSpanContext().getSpanId(), 
                              childSpan.getSpanContext().getSpanId());
                assertEquals(parentSpan.getSpanContext().getTraceId(), 
                           childSpan.getSpanContext().getTraceId());
                
            } finally {
                childSpan.end();
            }
            
        } finally {
            parentSpan.end();
        }
    }
    
    @Test
    void testSpanAttributes() {
        Tracer tracer = openTelemetry.getTracer("test-tracer", "1.0.0");
        
        Span span = tracer.spanBuilder("test-operation")
                .setAttribute("test.string", "value")
                .setAttribute("test.long", 123L)
                .setAttribute("test.double", 45.67)
                .setAttribute("test.boolean", true)
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Add more attributes
            span.setAttribute("dynamic.attribute", "added-later");
            
            // Simulate an error
            Exception testException = new RuntimeException("Test exception");
            span.recordException(testException);
            
        } finally {
            span.end();
        }
    }
}