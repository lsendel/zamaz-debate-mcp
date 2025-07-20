package com.zamaz.workflow.telemetry.service;

import com.zamaz.workflow.domain.port.TelemetryRepository;
import com.zamaz.workflow.telemetry.model.TelemetryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryProcessingPipeline {
    
    private final TelemetryRepository telemetryRepository;
    private final TelemetryValidationService validationService;
    private final TelemetryEnrichmentService enrichmentService;
    private final TelemetryRoutingService routingService;
    
    // Pipeline configuration
    private static final int BATCH_SIZE = 100;
    private static final Duration BATCH_TIMEOUT = Duration.ofMillis(100); // 10Hz = 100ms
    private static final int PARALLEL_WORKERS = 4;
    private static final int BUFFER_SIZE = 10000;
    
    // Metrics
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final Map<String, AtomicLong> deviceMetrics = new ConcurrentHashMap<>();
    
    // Backpressure handling
    private final Sinks.Many<TelemetryData> overflowSink = 
        Sinks.many().unicast().onBackpressureBuffer(Queues.<TelemetryData>small().get());
    
    public Flux<ProcessingResult> processTelemetryStream(Flux<TelemetryData> incomingData) {
        log.info("Starting telemetry processing pipeline");
        
        return incomingData
            // Handle backpressure
            .onBackpressureBuffer(
                BUFFER_SIZE,
                data -> {
                    droppedCount.incrementAndGet();
                    overflowSink.tryEmitNext(data);
                },
                BufferOverflowStrategy.DROP_OLDEST
            )
            // Validate data
            .flatMap(this::validateTelemetry)
            // Enrich data
            .flatMap(this::enrichTelemetry)
            // Batch for efficient database writes
            .bufferTimeout(BATCH_SIZE, BATCH_TIMEOUT)
            // Process batches in parallel
            .parallel(PARALLEL_WORKERS)
            .runOn(Schedulers.parallel())
            .flatMap(this::processBatch)
            // Merge back to sequential
            .sequential()
            // Route to workflows
            .flatMap(this::routeToWorkflows)
            // Monitor performance
            .doOnNext(result -> updateMetrics(result))
            // Handle errors
            .onErrorContinue((error, data) -> {
                log.error("Error processing telemetry: {}", error.getMessage());
                errorCount.incrementAndGet();
            });
    }
    
    private Mono<TelemetryData> validateTelemetry(TelemetryData data) {
        return Mono.fromCallable(() -> {
            if (!validationService.isValid(data)) {
                throw new ValidationException("Invalid telemetry data: " + data.getDeviceId());
            }
            return data;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.warn("Validation failed for device {}: {}", data.getDeviceId(), error.getMessage());
            return Mono.empty();
        });
    }
    
    private Mono<TelemetryData> enrichTelemetry(TelemetryData data) {
        return Mono.fromCallable(() -> enrichmentService.enrich(data))
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofMillis(50))
            .onErrorReturn(data); // Return original if enrichment fails
    }
    
    private Mono<BatchResult> processBatch(List<TelemetryData> batch) {
        if (batch.isEmpty()) {
            return Mono.just(BatchResult.empty());
        }
        
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            // Write to database
            int written = telemetryRepository.batchInsert(batch);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return BatchResult.builder()
                .batchSize(batch.size())
                .successCount(written)
                .failureCount(batch.size() - written)
                .processingTimeMs(duration)
                .telemetryData(batch)
                .build();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofSeconds(5))
        .onErrorReturn(BatchResult.failed(batch.size()));
    }
    
    private Mono<ProcessingResult> routeToWorkflows(BatchResult batchResult) {
        return Flux.fromIterable(batchResult.getTelemetryData())
            .flatMap(data -> routingService.routeToWorkflows(data))
            .collectList()
            .map(routingResults -> ProcessingResult.builder()
                .batchResult(batchResult)
                .routingResults(routingResults)
                .totalProcessed(processedCount.addAndGet(batchResult.getSuccessCount()))
                .build()
            );
    }
    
    private void updateMetrics(ProcessingResult result) {
        // Update device-specific metrics
        result.getBatchResult().getTelemetryData().forEach(data -> {
            deviceMetrics.computeIfAbsent(data.getDeviceId(), k -> new AtomicLong(0))
                .incrementAndGet();
        });
        
        // Log performance metrics periodically
        if (processedCount.get() % 10000 == 0) {
            logPerformanceMetrics();
        }
    }
    
    private void logPerformanceMetrics() {
        double throughput = processedCount.get() / (System.currentTimeMillis() / 1000.0);
        log.info("Pipeline metrics - Processed: {}, Errors: {}, Dropped: {}, Throughput: {:.2f} msg/s",
            processedCount.get(), errorCount.get(), droppedCount.get(), throughput);
        
        // Log top devices
        deviceMetrics.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> 
                log.debug("Device {} processed {} messages", entry.getKey(), entry.getValue())
            );
    }
    
    // Recovery mechanism for dropped messages
    public Flux<TelemetryData> getOverflowStream() {
        return overflowSink.asFlux()
            .doOnNext(data -> log.warn("Processing overflow data for device: {}", data.getDeviceId()));
    }
    
    public PipelineStats getStats() {
        return PipelineStats.builder()
            .processedCount(processedCount.get())
            .errorCount(errorCount.get())
            .droppedCount(droppedCount.get())
            .deviceCount(deviceMetrics.size())
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class BatchResult {
        private final int batchSize;
        private final int successCount;
        private final int failureCount;
        private final long processingTimeMs;
        private final List<TelemetryData> telemetryData;
        
        public static BatchResult empty() {
            return BatchResult.builder()
                .batchSize(0)
                .successCount(0)
                .failureCount(0)
                .processingTimeMs(0)
                .telemetryData(new ArrayList<>())
                .build();
        }
        
        public static BatchResult failed(int size) {
            return BatchResult.builder()
                .batchSize(size)
                .successCount(0)
                .failureCount(size)
                .processingTimeMs(0)
                .telemetryData(new ArrayList<>())
                .build();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ProcessingResult {
        private final BatchResult batchResult;
        private final List<RoutingResult> routingResults;
        private final long totalProcessed;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RoutingResult {
        private final String workflowId;
        private final String deviceId;
        private final boolean triggered;
        private final String reason;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PipelineStats {
        private final long processedCount;
        private final long errorCount;
        private final long droppedCount;
        private final int deviceCount;
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
    
    public enum BufferOverflowStrategy {
        DROP_OLDEST,
        DROP_LATEST,
        ERROR
    }
}

// Supporting services
@Service
@Slf4j
class TelemetryValidationService {
    public boolean isValid(TelemetryData data) {
        // Validate required fields
        if (data.getDeviceId() == null || data.getTimestamp() == null) {
            return false;
        }
        
        // Validate location bounds
        GeoLocation loc = data.getLocation();
        if (loc.getLatitude() < -90 || loc.getLatitude() > 90 ||
            loc.getLongitude() < -180 || loc.getLongitude() > 180) {
            return false;
        }
        
        // Validate metrics
        return !data.getMetrics().isEmpty();
    }
}

@Service
@Slf4j
class TelemetryEnrichmentService {
    private final Map<String, Object> deviceCache = new ConcurrentHashMap<>();
    
    public TelemetryData enrich(TelemetryData data) {
        // Add device metadata from cache
        Object deviceInfo = deviceCache.get(data.getDeviceId());
        
        // Add computed fields
        Map<String, Object> enrichedMetrics = new HashMap<>(data.getMetrics());
        
        // Calculate derived metrics
        if (enrichedMetrics.containsKey("temperature") && enrichedMetrics.containsKey("humidity")) {
            double temp = (Double) enrichedMetrics.get("temperature");
            double humidity = (Double) enrichedMetrics.get("humidity");
            double heatIndex = calculateHeatIndex(temp, humidity);
            enrichedMetrics.put("heatIndex", heatIndex);
        }
        
        return TelemetryData.builder()
            .id(data.getId())
            .deviceId(data.getDeviceId())
            .timestamp(data.getTimestamp())
            .location(data.getLocation())
            .metrics(enrichedMetrics)
            .metadata(data.getMetadata())
            .build();
    }
    
    private double calculateHeatIndex(double temp, double humidity) {
        // Simplified heat index calculation
        return temp + (0.5 * humidity);
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
class TelemetryRoutingService {
    private final WorkflowTriggerService workflowTriggerService;
    
    public Mono<TelemetryProcessingPipeline.RoutingResult> routeToWorkflows(TelemetryData data) {
        return Mono.fromCallable(() -> {
            // Check workflow triggers
            List<String> triggeredWorkflows = workflowTriggerService.checkTriggers(data);
            
            if (!triggeredWorkflows.isEmpty()) {
                return TelemetryProcessingPipeline.RoutingResult.builder()
                    .workflowId(triggeredWorkflows.get(0))
                    .deviceId(data.getDeviceId())
                    .triggered(true)
                    .reason("Threshold exceeded")
                    .build();
            }
            
            return TelemetryProcessingPipeline.RoutingResult.builder()
                .deviceId(data.getDeviceId())
                .triggered(false)
                .reason("No conditions met")
                .build();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}