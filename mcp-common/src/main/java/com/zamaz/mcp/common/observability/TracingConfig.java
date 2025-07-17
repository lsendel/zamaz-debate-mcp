package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.instrumentation.spring.webmvc.v5_3.SpringWebMvcTelemetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry tracing configuration for distributed tracing
 */
@Configuration
@ConditionalOnProperty(value = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig implements WebMvcConfigurer {
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Value("${tracing.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;
    
    @Value("${tracing.sampling.probability:1.0}")
    private double samplingProbability;
    
    @Bean
    public OpenTelemetry openTelemetry() {
        // Create Jaeger exporter
        SpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint(jaegerEndpoint)
            .setTimeout(30, TimeUnit.SECONDS)
            .build();
        
        // Create resource with service information
        Resource resource = Resource.getDefault()
            .merge(Resource.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .put(ResourceAttributes.SERVICE_VERSION, getClass().getPackage().getImplementationVersion())
                .put("environment", System.getProperty("spring.profiles.active", "default"))
                .build());
        
        // Create tracer provider with batch processor
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                .setMaxQueueSize(2048)
                .setMaxExportBatchSize(512)
                .build())
            .setResource(resource)
            .setSampler(Sampler.traceIdRatioBased(samplingProbability))
            .build();
        
        // Build OpenTelemetry SDK
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));
        
        return openTelemetry;
    }
    
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, getClass().getPackage().getImplementationVersion());
    }
    
    @Bean
    public Filter tracingFilter(OpenTelemetry openTelemetry) {
        return SpringWebMvcTelemetry.create(openTelemetry).createServletFilter();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add tracing interceptor for Spring MVC
        SpringWebMvcTelemetry telemetry = SpringWebMvcTelemetry.create(openTelemetry());
        registry.addInterceptor(telemetry.createHandlerInterceptor());
    }
    
    /**
     * Custom span processor for adding common attributes
     */
    @Bean
    public CustomSpanProcessor customSpanProcessor() {
        return new CustomSpanProcessor();
    }
}