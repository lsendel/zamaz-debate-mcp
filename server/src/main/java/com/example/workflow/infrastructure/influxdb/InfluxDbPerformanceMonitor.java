package com.example.workflow.infrastructure.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring for InfluxDB telemetry operations
 * Provides metrics collection, health checks, and performance analysis
 */
@Component
public class InfluxDbPerformanceMonitor implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbPerformanceMonitor.class);
    
    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final QueryApi queryApi;
    private final InfluxDbBatchProcessor batchProcessor;
    private final MeterRegistry meterRegistry;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.organization:workflow-org}")
    private String organization;
    
    @Value("${influxdb.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    // Performance metrics
    private final AtomicLong writeOperations = new AtomicLong(0);
    private final AtomicLong readOperations = new AtomicLong(0);
    private final AtomicLong writeErrors = new AtomicLong(0);
    private final AtomicLong readErrors = new AtomicLong(0);
    private final AtomicLong connectionErrors = new AtomicLong(0);
    
    // Timing metrics
    private Timer writeTimer;
    private Timer readTimer;
    private Counter writeCounter;
    private Counter readCounter;
    private Counter errorCounter;
    
    // Health status
    private volatile boolean isHealthy = true;
    private volatile String lastErrorMessage = "";
    private volatile Instant lastHealthCheck = Instant.now();
    
    @Autowired
    public InfluxDbPerformanceMonitor(InfluxDBClient influxDBClient, 
                                    InfluxDbBatchProcessor batchProcessor,
                                    MeterRegistry meterRegistry) {
        this.influxDBClient = influxDBClient;
        this.writeApi = influxDBClient.getWriteApiBlocking();
        this.queryApi = influxDBClient.getQueryApi();
        this.batchProcessor = batchProcessor;
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void initializeMetrics() {
        if (!monitoringEnabled) {
            logger.info("InfluxDB performance monitoring is disabled");
            return;
        }
        
        logger.info("Initializing InfluxDB performance monitoring...");
        
        // Initialize Micrometer metrics
        writeTimer = Timer.builder("influxdb.write.duration")
            .description("Time taken for InfluxDB write operations")
            .register(meterRegistry);
        
        readTimer = Timer.builder("influxdb.read.duration")
            .description("Time taken for InfluxDB read operations")
            .register(meterRegistry);
        
        writeCounter = Counter.builder("influxdb.write.operations")
            .description("Number of InfluxDB write operations")
            .register(meterRegistry);
        
        readCounter = Counter.builder("influxdb.read.operations")
            .description("Number of InfluxDB read operations")
            .register(meterRegistry);
        
        errorCounter = Counter.builder("influxdb.errors")
            .description("Number of InfluxDB errors")
            .tag("type", "all")
            .register(meterRegistry);
        
        // Register gauges for real-time metrics
        Gauge.builder("influxdb.batch.queue.size")
            .description("Current batch queue size")
            .register(meterRegistry, this, monitor -> monitor.getBatchQueueSize());
        
        Gauge.builder("influxdb.batch.throughput")
            .description("Batch processing throughput (points/sec)")
            .register(meterRegistry, this, monitor -> monitor.getBatchThroughput());
        
        Gauge.builder("influxdb.connection.health")
            .description("InfluxDB connection health (1=healthy, 0=unhealthy)")
            .register(meterRegistry, this, monitor -> monitor.isHealthy ? 1.0 : 0.0);
        
        logger.info("InfluxDB performance monitoring initialized successfully");
    }
    
    /**
     * Record write operation metrics
     */
    public void recordWriteOperation(Duration duration, boolean success) {
        if (!monitoringEnabled) return;
        
        writeOperations.incrementAndGet();
        writeCounter.increment();
        writeTimer.record(duration);
        
        if (!success) {
            writeErrors.incrementAndGet();
            errorCounter.increment();
        }
    }
    
    /**
     * Record read operation metrics
     */
    public void recordReadOperation(Duration duration, boolean success) {
        if (!monitoringEnabled) return;
        
        readOperations.incrementAndGet();
        readCounter.increment();
        readTimer.record(duration);
        
        if (!success) {
            readErrors.incrementAndGet();
            errorCounter.increment();
        }
    }
    
    /**
     * Record connection error
     */
    public void recordConnectionError(String errorMessage) {
        if (!monitoringEnabled) return;
        
        connectionErrors.incrementAndGet();
        errorCounter.increment();
        lastErrorMessage = errorMessage;
        isHealthy = false;
        
        logger.warn("InfluxDB connection error recorded: {}", errorMessage);
    }
    
    /**
     * Perform health check - scheduled every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    public void performHealthCheck() {
        if (!monitoringEnabled) return;
        
        try {
            // Simple ping query to check connectivity
            String pingQuery = String.format(
                "from(bucket: \"%s\") |> range(start: -1m) |> limit(n: 1)", bucket);
            
            Instant start = Instant.now();
            List<FluxTable> result = queryApi.query(pingQuery, organization);
            Duration duration = Duration.between(start, Instant.now());
            
            recordReadOperation(duration, true);
            
            // Check batch processor health
            boolean batchHealthy = batchProcessor.isHealthy();
            
            isHealthy = batchHealthy;
            lastHealthCheck = Instant.now();
            
            if (isHealthy) {
                lastErrorMessage = "";
            }
            
            logger.debug("InfluxDB health check completed successfully in {}ms", duration.toMillis());
            
        } catch (Exception e) {
            recordConnectionError("Health check failed: " + e.getMessage());
            logger.error("InfluxDB health check failed", e);
        }
    }
    
    /**
     * Collect and store performance metrics - scheduled every 5 minutes
     */
    @Scheduled(fixedDelay = 300000)
    public void collectPerformanceMetrics() {
        if (!monitoringEnabled) return;
        
        try {
            PerformanceMetrics metrics = getCurrentMetrics();
            storePerformanceMetrics(metrics);
            
            logger.info("Performance metrics collected - Writes: {}, Reads: {}, Errors: {}, Throughput: {}/sec",
                metrics.writeOperations(), metrics.readOperations(), 
                metrics.totalErrors(), String.format("%.2f", metrics.throughput()));
            
        } catch (Exception e) {
            logger.error("Failed to collect performance metrics", e);
        }
    }
    
    /**
     * Store performance metrics in InfluxDB
     */
    private void storePerformanceMetrics(PerformanceMetrics metrics) {
        try {
            Point point = Point.measurement("influxdb_performance")
                .time(Instant.now(), WritePrecision.MS)
                .addTag("instance", "workflow-server")
                .addField("write_operations", metrics.writeOperations())
                .addField("read_operations", metrics.readOperations())
                .addField("write_errors", metrics.writeErrors())
                .addField("read_errors", metrics.readErrors())
                .addField("connection_errors", metrics.connectionErrors())
                .addField("total_errors", metrics.totalErrors())
                .addField("throughput", metrics.throughput())
                .addField("batch_queue_size", metrics.batchQueueSize())
                .addField("is_healthy", metrics.isHealthy() ? 1 : 0);
            
            writeApi.writePoint(bucket, organization, point);
            
        } catch (Exception e) {
            logger.warn("Failed to store performance metrics: {}", e.getMessage());
        }
    }
    
    /**
     * Get current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        InfluxDbBatchProcessor.BatchProcessingStats batchStats = batchProcessor.getStats();
        
        return new PerformanceMetrics(
            writeOperations.get(),
            readOperations.get(),
            writeErrors.get(),
            readErrors.get(),
            connectionErrors.get(),
            writeErrors.get() + readErrors.get() + connectionErrors.get(),
            batchStats.throughputPerSecond(),
            batchStats.currentQueueSize(),
            isHealthy,
            lastHealthCheck,
            lastErrorMessage
        );
    }
    
    /**
     * Get batch queue size for gauge
     */
    private long getBatchQueueSize() {
        return batchProcessor.getStats().currentQueueSize();
    }
    
    /**
     * Get batch throughput for gauge
     */
    private double getBatchThroughput() {
        return batchProcessor.getStats().throughputPerSecond();
    }
    
    /**
     * Get performance statistics for the last period
     */
    public PerformanceStatistics getPerformanceStatistics(Duration period) {
        try {
            Instant start = Instant.now().minus(period);
            
            String query = String.format("""
                from(bucket: "%s")
                  |> range(start: %s)
                  |> filter(fn: (r) => r._measurement == "influxdb_performance")
                  |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
                """, bucket, start);
            
            List<FluxTable> tables = queryApi.query(query, organization);
            
            // Process results to calculate statistics
            return calculateStatisticsFromTables(tables);
            
        } catch (Exception e) {
            logger.error("Failed to get performance statistics", e);
            return new PerformanceStatistics(0.0, 0.0, 0.0, 0.0, 0L, 0L);
        }
    }
    
    /**
     * Calculate statistics from Flux query results
     */
    private PerformanceStatistics calculateStatisticsFromTables(List<FluxTable> tables) {
        double avgThroughput = 0.0;
        double maxThroughput = 0.0;
        double avgQueueSize = 0.0;
        double errorRate = 0.0;
        long totalOperations = 0L;
        long totalErrors = 0L;
        
        // Simple implementation - in practice, you'd process the Flux results properly
        return new PerformanceStatistics(
            avgThroughput, maxThroughput, avgQueueSize, errorRate, totalOperations, totalErrors);
    }
    
    /**
     * Reset performance counters
     */
    public void resetCounters() {
        writeOperations.set(0);
        readOperations.set(0);
        writeErrors.set(0);
        readErrors.set(0);
        connectionErrors.set(0);
        lastErrorMessage = "";
        
        logger.info("InfluxDB performance counters reset");
    }
    
    @Override
    public Health health() {
        Health.Builder builder = isHealthy ? Health.up() : Health.down();
        
        PerformanceMetrics metrics = getCurrentMetrics();
        
        return builder
            .withDetail("writeOperations", metrics.writeOperations())
            .withDetail("readOperations", metrics.readOperations())
            .withDetail("totalErrors", metrics.totalErrors())
            .withDetail("throughput", String.format("%.2f/sec", metrics.throughput()))
            .withDetail("batchQueueSize", metrics.batchQueueSize())
            .withDetail("lastHealthCheck", metrics.lastHealthCheck())
            .withDetail("lastError", metrics.lastErrorMessage())
            .build();
    }
    
    /**
     * Performance metrics record
     */
    public record PerformanceMetrics(
        long writeOperations,
        long readOperations,
        long writeErrors,
        long readErrors,
        long connectionErrors,
        long totalErrors,
        double throughput,
        long batchQueueSize,
        boolean isHealthy,
        Instant lastHealthCheck,
        String lastErrorMessage
    ) {}
    
    /**
     * Performance statistics record
     */
    public record PerformanceStatistics(
        double averageThroughput,
        double maxThroughput,
        double averageQueueSize,
        double errorRate,
        long totalOperations,
        long totalErrors
    ) {}
}