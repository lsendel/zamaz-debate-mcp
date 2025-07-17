package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.SpringWebfluxTelemetry;
import io.opentelemetry.instrumentation.spring.webmvc.v5_3.SpringWebMvcTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced distributed tracing configuration with multiple exporters and advanced features
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.tracing")
@Data
@Slf4j
public class DistributedTracingConfig implements WebMvcConfigurer {

    /**
     * Whether distributed tracing is enabled
     */
    private boolean enabled = true;

    /**
     * Service instance ID
     */
    @Value("${spring.application.instance-id:${random.uuid}}")
    private String instanceId;

    /**
     * Service version
     */
    @Value("${spring.application.version:1.0.0}")
    private String serviceVersion;

    /**
     * Service environment
     */
    @Value("${spring.profiles.active:default}")
    private String environment;

    /**
     * Tracing exporters configuration
     */
    private ExportersConfig exporters = new ExportersConfig();

    /**
     * Sampling configuration
     */
    private AdvancedSamplingConfig sampling = new AdvancedSamplingConfig();

    /**
     * Propagation configuration
     */
    private PropagationConfig propagation = new PropagationConfig();

    /**
     * Baggage configuration
     */
    private BaggageConfig baggage = new BaggageConfig();

    /**
     * Create enhanced OpenTelemetry instance with distributed tracing
     */
    @Bean
    @Primary
    public OpenTelemetry distributedOpenTelemetry(
            @Value("${spring.application.name}") String serviceName,
            Environment env) {
        
        if (!enabled) {
            log.info("Distributed tracing is disabled");
            return OpenTelemetry.noop();
        }

        log.info("Initializing distributed tracing for service: {} (instance: {})", serviceName, instanceId);

        // Build resource with comprehensive attributes
        Resource resource = buildResource(serviceName, env);

        // Create span exporters
        List<SpanExporter> spanExporters = createSpanExporters();
        
        // Build tracer provider with custom span processor
        SdkTracerProvider tracerProvider = buildTracerProvider(resource, spanExporters);

        // Build meter provider with Prometheus exporter
        SdkMeterProvider meterProvider = buildMeterProvider(resource);

        // Configure context propagators
        ContextPropagators propagators = buildPropagators();

        // Build OpenTelemetry SDK
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setPropagators(propagators)
                .buildAndRegisterGlobal();

        log.info("Distributed tracing initialized successfully with {} exporters", spanExporters.size());

        return openTelemetry;
    }

    /**
     * Build comprehensive resource information
     */
    private Resource buildResource(String serviceName, Environment env) {
        Attributes attributes = Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                .put(ResourceAttributes.SERVICE_INSTANCE_ID, instanceId)
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, environment)
                .put(ResourceAttributes.HOST_NAME, getHostname())
                .put(ResourceAttributes.PROCESS_PID, getProcessId())
                .put("service.namespace", "mcp")
                .put("service.layer", getServiceLayer(serviceName))
                .put("cloud.provider", env.getProperty("cloud.provider", "local"))
                .put("cloud.region", env.getProperty("cloud.region", "local"))
                .build();

        return Resource.create(attributes);
    }

    /**
     * Create span exporters based on configuration
     */
    private List<SpanExporter> createSpanExporters() {
        List<SpanExporter> exporters = new ArrayList<>();

        // OTLP exporter (recommended)
        if (this.exporters.getOtlp().isEnabled()) {
            try {
                OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
                        .setEndpoint(this.exporters.getOtlp().getEndpoint())
                        .setTimeout(this.exporters.getOtlp().getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .setCompression(this.exporters.getOtlp().getCompression())
                        .build();
                exporters.add(otlpExporter);
                log.info("OTLP span exporter configured: {}", this.exporters.getOtlp().getEndpoint());
            } catch (Exception e) {
                log.error("Failed to create OTLP exporter", e);
            }
        }

        // Jaeger exporter (backward compatibility)
        if (this.exporters.getJaeger().isEnabled()) {
            try {
                JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                        .setEndpoint(this.exporters.getJaeger().getEndpoint())
                        .setTimeout(this.exporters.getJaeger().getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .build();
                exporters.add(jaegerExporter);
                log.info("Jaeger span exporter configured: {}", this.exporters.getJaeger().getEndpoint());
            } catch (Exception e) {
                log.error("Failed to create Jaeger exporter", e);
            }
        }

        // Logging exporter (for debugging)
        if (this.exporters.getLogging().isEnabled()) {
            exporters.add(LoggingSpanExporter.create());
            log.info("Logging span exporter enabled");
        }

        if (exporters.isEmpty()) {
            log.warn("No span exporters configured, using logging exporter as fallback");
            exporters.add(LoggingSpanExporter.create());
        }

        return exporters;
    }

    /**
     * Build tracer provider with span processors
     */
    private SdkTracerProvider buildTracerProvider(Resource resource, List<SpanExporter> exporters) {
        SdkTracerProvider.Builder builder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(buildSampler());

        // Add custom span processor for context enrichment
        builder.addSpanProcessor(new CustomSpanProcessor());

        // Add batch span processors for each exporter
        for (SpanExporter exporter : exporters) {
            SpanProcessor processor;
            if (this.exporters.isUseBatchProcessor()) {
                processor = BatchSpanProcessor.builder(exporter)
                        .setScheduleDelay(this.exporters.getBatchDelay())
                        .setMaxQueueSize(this.exporters.getMaxQueueSize())
                        .setMaxExportBatchSize(this.exporters.getMaxBatchSize())
                        .setExporterTimeout(this.exporters.getExportTimeout())
                        .build();
            } else {
                processor = SimpleSpanProcessor.create(exporter);
            }
            builder.addSpanProcessor(processor);
        }

        return builder.build();
    }

    /**
     * Build meter provider with Prometheus exporter
     */
    private SdkMeterProvider buildMeterProvider(Resource resource) {
        SdkMeterProvider.Builder builder = SdkMeterProvider.builder()
                .setResource(resource);

        if (exporters.getPrometheus().isEnabled()) {
            try {
                MetricReader prometheusReader = PrometheusHttpServer.builder()
                        .setHost(exporters.getPrometheus().getHost())
                        .setPort(exporters.getPrometheus().getPort())
                        .build();
                builder.registerMetricReader(prometheusReader);
                log.info("Prometheus metrics exporter configured on port {}", 
                    exporters.getPrometheus().getPort());
            } catch (Exception e) {
                log.error("Failed to create Prometheus exporter", e);
            }
        }

        return builder.build();
    }

    /**
     * Build advanced sampler with multiple strategies
     */
    private Sampler buildSampler() {
        switch (sampling.getStrategy().toLowerCase()) {
            case "always_on":
                return Sampler.alwaysOn();
            case "always_off":
                return Sampler.alwaysOff();
            case "trace_id_ratio":
                return Sampler.traceIdRatioBased(sampling.getRatio());
            case "parent_based":
                Sampler rootSampler = Sampler.traceIdRatioBased(sampling.getRatio());
                return Sampler.parentBasedBuilder(rootSampler)
                        .setRemoteParentSampled(Sampler.alwaysOn())
                        .setRemoteParentNotSampled(Sampler.alwaysOff())
                        .setLocalParentSampled(Sampler.alwaysOn())
                        .setLocalParentNotSampled(Sampler.alwaysOff())
                        .build();
            case "rate_limiting":
                // Custom rate limiting sampler
                return new RateLimitingSampler(sampling.getMaxTracesPerSecond());
            default:
                log.warn("Unknown sampling strategy: {}, using trace_id_ratio", sampling.getStrategy());
                return Sampler.traceIdRatioBased(sampling.getRatio());
        }
    }

    /**
     * Build context propagators with multiple formats
     */
    private ContextPropagators buildPropagators() {
        List<TextMapPropagator> propagators = new ArrayList<>();

        // W3C Trace Context (standard)
        if (propagation.isW3cTraceContext()) {
            propagators.add(W3CTraceContextPropagator.getInstance());
        }

        // W3C Baggage
        if (propagation.isW3cBaggage()) {
            propagators.add(W3CBaggagePropagator.getInstance());
        }

        // B3 (Zipkin compatibility)
        if (propagation.isB3()) {
            propagators.add(B3Propagator.injectingMultiHeaders());
        }

        // Jaeger (backward compatibility)
        if (propagation.isJaeger()) {
            // Note: Jaeger propagator would need to be implemented or imported
            log.warn("Jaeger propagator requested but not implemented in this example");
        }

        return ContextPropagators.create(
            TextMapPropagator.composite(propagators.toArray(new TextMapPropagator[0]))
        );
    }

    /**
     * Configure Spring WebMVC tracing
     */
    @Bean
    @ConditionalOnProperty(value = "mcp.tracing.spring.webmvc.enabled", havingValue = "true", matchIfMissing = true)
    public Filter webMvcTracingFilter(OpenTelemetry openTelemetry) {
        SpringWebMvcTelemetry telemetry = SpringWebMvcTelemetry.builder(openTelemetry)
                .setCapturedRequestHeaders(Arrays.asList("X-Request-ID", "X-Organization-ID", "X-User-ID"))
                .setCapturedResponseHeaders(Arrays.asList("X-Request-ID"))
                .build();
        
        return telemetry.createServletFilter();
    }

    /**
     * Configure Spring WebFlux tracing (for reactive applications)
     */
    @Bean
    @ConditionalOnProperty(value = "mcp.tracing.spring.webflux.enabled", havingValue = "true")
    public SpringWebfluxTelemetry webfluxTelemetry(OpenTelemetry openTelemetry) {
        return SpringWebfluxTelemetry.builder(openTelemetry)
                .setCapturedRequestHeaders(Arrays.asList("X-Request-ID", "X-Organization-ID", "X-User-ID"))
                .setCapturedResponseHeaders(Arrays.asList("X-Request-ID"))
                .build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add tracing interceptor for additional context
        registry.addInterceptor(new TracingInterceptor());
    }

    // Helper methods

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private long getProcessId() {
        return ProcessHandle.current().pid();
    }

    private String getServiceLayer(String serviceName) {
        if (serviceName.contains("gateway")) return "gateway";
        if (serviceName.contains("controller")) return "controller";
        if (serviceName.contains("service")) return "service";
        if (serviceName.contains("repository")) return "data";
        return "application";
    }

    // Configuration classes

    @Data
    public static class ExportersConfig {
        private OtlpConfig otlp = new OtlpConfig();
        private JaegerConfig jaeger = new JaegerConfig();
        private PrometheusConfig prometheus = new PrometheusConfig();
        private LoggingConfig logging = new LoggingConfig();
        private boolean useBatchProcessor = true;
        private Duration batchDelay = Duration.ofSeconds(5);
        private int maxQueueSize = 2048;
        private int maxBatchSize = 512;
        private Duration exportTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class OtlpConfig {
        private boolean enabled = true;
        private String endpoint = "http://otel-collector:4317";
        private Duration timeout = Duration.ofSeconds(10);
        private String compression = "gzip";
    }

    @Data
    public static class JaegerConfig {
        private boolean enabled = false;
        private String endpoint = "http://jaeger:14250";
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class PrometheusConfig {
        private boolean enabled = true;
        private String host = "0.0.0.0";
        private int port = 9464;
    }

    @Data
    public static class LoggingConfig {
        private boolean enabled = false;
        private String level = "INFO";
    }

    @Data
    public static class AdvancedSamplingConfig {
        private String strategy = "parent_based"; // always_on, always_off, trace_id_ratio, parent_based, rate_limiting
        private double ratio = 0.1; // For trace_id_ratio strategy
        private int maxTracesPerSecond = 100; // For rate_limiting strategy
    }

    @Data
    public static class PropagationConfig {
        private boolean w3cTraceContext = true;
        private boolean w3cBaggage = true;
        private boolean b3 = true;
        private boolean jaeger = false;
    }

    @Data
    public static class BaggageConfig {
        private boolean enabled = true;
        private List<String> allowedKeys = Arrays.asList("user.id", "organization.id", "request.id", "session.id");
        private int maxEntries = 10;
        private int maxValueLength = 256;
    }
}