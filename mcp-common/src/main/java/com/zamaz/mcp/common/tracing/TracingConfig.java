package com.zamaz.mcp.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for distributed tracing.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.tracing")
@Data
public class TracingConfig {

    /**
     * Whether to enable tracing.
     */
    private boolean enabled = true;

    /**
     * Jaeger configuration.
     */
    private JaegerConfig jaeger = new JaegerConfig();

    /**
     * Sampling configuration.
     */
    private SamplingConfig sampling = new SamplingConfig();

    /**
     * Creates an OpenTelemetry instance.
     *
     * @param serviceName the service name
     * @return the OpenTelemetry instance
     */
    @Bean
    public OpenTelemetry openTelemetry(@Value("${spring.application.name}") String serviceName) {
        if (!enabled) {
            return OpenTelemetry.noop();
        }
        
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )));
        
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(jaeger.getEndpoint())
                .build();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .setResource(resource)
                .build();
        
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    /**
     * Jaeger configuration.
     */
    @Data
    public static class JaegerConfig {
        private String endpoint = "http://jaeger:14250";
    }

    /**
     * Sampling configuration.
     */
    @Data
    public static class SamplingConfig {
        private double probability = 1.0;
    }
}
