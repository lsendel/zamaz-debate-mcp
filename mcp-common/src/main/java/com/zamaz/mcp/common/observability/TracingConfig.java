package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for OpenTelemetry tracing and metrics.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.observability")
@Data
@Slf4j
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
     * Metrics configuration.
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * Creates an OpenTelemetry instance.
     *
     * @param serviceName the service name
     * @return the OpenTelemetry instance
     */
    @Bean
    public OpenTelemetry openTelemetry(@Value("${spring.application.name}") String serviceName) {
        if (!enabled) {
            log.info("Tracing is disabled. Using OpenTelemetry noop implementation.");
            return OpenTelemetry.noop();
        }
        
        log.info("Initializing OpenTelemetry for service: {}", serviceName);
        
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )));
        
        // Configure Jaeger exporter
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(jaeger.getEndpoint())
                .setTimeout(jaeger.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
        
        // Configure tracer provider with appropriate sampler
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter)
                        .setScheduleDelay(jaeger.getExportInterval())
                        .setMaxQueueSize(jaeger.getMaxQueueSize())
                        .setMaxExportBatchSize(jaeger.getMaxBatchSize())
                        .build())
                .setResource(resource)
                .setSampler(getSampler())
                .build();
        
        // Configure metrics if enabled
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .build();
        
        if (metrics.isEnabled()) {
            try {
                OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                        .setEndpoint(metrics.getEndpoint())
                        .setTimeout(metrics.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .build();
                
                meterProvider = SdkMeterProvider.builder()
                        .setResource(resource)
                        .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                                .setInterval(metrics.getExportInterval())
                                .build())
                        .build();
                
                log.info("Metrics export configured to endpoint: {}", metrics.getEndpoint());
            } catch (Exception e) {
                log.error("Failed to configure metrics export. Using default meter provider.", e);
            }
        }
        
        // Build and register the OpenTelemetry SDK
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        
        log.info("OpenTelemetry initialized successfully for service: {}", serviceName);
        
        return openTelemetry;
    }
    
    /**
     * Get the appropriate sampler based on configuration.
     *
     * @return the sampler
     */
    private Sampler getSampler() {
        if (sampling.getType().equalsIgnoreCase("always")) {
            return Sampler.alwaysOn();
        } else if (sampling.getType().equalsIgnoreCase("never")) {
            return Sampler.alwaysOff();
        } else if (sampling.getType().equalsIgnoreCase("ratio")) {
            return Sampler.traceIdRatioBased(sampling.getRatio());
        } else {
            // Default to parent-based sampling with ratio
            return Sampler.parentBased(Sampler.traceIdRatioBased(sampling.getRatio()));
        }
    }

    /**
     * Jaeger configuration.
     */
    @Data
    public static class JaegerConfig {
        private String endpoint = "http://jaeger:14250";
        private Duration timeout = Duration.ofSeconds(10);
        private Duration exportInterval = Duration.ofSeconds(5);
        private int maxQueueSize = 2048;
        private int maxBatchSize = 512;
    }

    /**
     * Sampling configuration.
     */
    @Data
    public static class SamplingConfig {
        private String type = "ratio"; // always, never, ratio, parent
        private double ratio = 1.0;
    }
    
    /**
     * Metrics configuration.
     */
    @Data
    public static class MetricsConfig {
        private boolean enabled = true;
        private String endpoint = "http://otel-collector:4317";
        private Duration timeout = Duration.ofSeconds(10);
        private Duration exportInterval = Duration.ofSeconds(60);
    }
}
