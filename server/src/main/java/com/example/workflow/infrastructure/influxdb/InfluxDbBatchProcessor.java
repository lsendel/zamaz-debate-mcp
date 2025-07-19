package com.example.workflow.infrastructure.influxdb;

import com.example.workflow.domain.TelemetryData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.events.WriteErrorEvent;
import com.influxdb.client.write.events.WriteSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * High-performance batch processor for InfluxDB telemetry data ingestion
 * Handles 10Hz telemetry data with optimized batch writing and backpressure management
 */
@Component
public class InfluxDbBatchProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbBatchProcessor.class);
    
    private final InfluxDBClient influxDBClient;
    private WriteApi writeApi;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.organization:workflow-org}")
    private String organization;
    
    @Value("${influxdb.batch.size:1000}")
    private int batchSize;
    
    @Value("${influxdb.batch.flush-interval:1000}")
    private int flushIntervalMs;
    
    @Value("${influxdb.batch.queue-capacity:10000}")
    private int queueCapacity;
    
    @Value("${influxdb.batch.max-retries:3}")
    private int maxRetries;
    
    // Batch processing queue
    private final BlockingQueue<TelemetryData> dataQueue;
    private final ReentrantLock batchLock = new ReentrantLock();
    
    // Performance metrics
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong batchesProcessed = new AtomicLong(0);
    private final AtomicLong queueSize = new AtomicLong(0);
    private volatile long lastFlushTime = System.currentTimeMillis();
    
    // Batch buffer
    private final List<TelemetryData> currentBatch = new ArrayList<>();
    
    @Autowired
    public InfluxDbBatchProcessor(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
        this.dataQueue = new LinkedBlockingQueue<>(queueCapacity);
    }
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing InfluxDB batch processor with batch size: {}, flush interval: {}ms", 
            batchSize, flushIntervalMs);
        
        // Create async write API with optimized settings
        this.writeApi = influxDBClient.makeWriteApi();
        
        // Configure event listeners
        configureEventListeners();
        
        logger.info("InfluxDB batch processor initialized successfully");
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down InfluxDB batch processor...");
        
        // Flush remaining data
        flushCurrentBatch();
        
        // Close write API
        if (writeApi != null) {
            writeApi.close();
        }
        
        logger.info("InfluxDB batch processor shutdown completed. Total processed: {}, Total errors: {}", 
            totalProcessed.get(), totalErrors.get());
    }
    
    /**
     * Configure event listeners for write operations
     */
    private void configureEventListeners() {
        writeApi.listenEvents(WriteSuccessEvent.class, event -> {
            logger.debug("Batch write successful: {} points", event.getLineProtocol().split("\n").length);
            batchesProcessed.incrementAndGet();
        });
        
        writeApi.listenEvents(WriteErrorEvent.class, event -> {
            logger.error("Batch write failed: {}", event.getThrowable().getMessage());
            totalErrors.incrementAndGet();
            
            // Handle retry logic if needed
            handleWriteError(event);
        });
    }
    
    /**
     * Add telemetry data to batch processing queue
     */
    public boolean addTelemetryData(TelemetryData data) {
        if (data == null) {
            return false;
        }
        
        boolean added = dataQueue.offer(data);
        if (added) {
            queueSize.incrementAndGet();
        } else {
            logger.warn("Batch queue is full, dropping telemetry data for device: {}", 
                data.getDeviceId().getValue());
        }
        
        return added;
    }
    
    /**
     * Add multiple telemetry data points to batch processing queue
     */
    public int addTelemetryDataBatch(List<TelemetryData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        int added = 0;
        for (TelemetryData data : dataList) {
            if (addTelemetryData(data)) {
                added++;
            }
        }
        
        return added;
    }
    
    /**
     * Process queued data in batches - scheduled every 100ms for 10Hz processing
     */
    @Scheduled(fixedDelay = 100)
    public void processBatch() {
        try {
            batchLock.lock();
            
            // Drain queue into current batch
            List<TelemetryData> batch = new ArrayList<>();
            dataQueue.drainTo(batch, batchSize);
            
            if (batch.isEmpty()) {
                return;
            }
            
            queueSize.addAndGet(-batch.size());
            
            // Process the batch
            processTelemetryBatch(batch);
            
            totalProcessed.addAndGet(batch.size());
            
        } catch (Exception e) {
            logger.error("Error processing telemetry batch", e);
            totalErrors.incrementAndGet();
        } finally {
            batchLock.unlock();
        }
    }
    
    /**
     * Force flush current batch - scheduled every flush interval
     */
    @Scheduled(fixedDelayString = "${influxdb.batch.flush-interval:1000}")
    public void flushCurrentBatch() {
        try {
            batchLock.lock();
            
            if (currentBatch.isEmpty()) {
                return;
            }
            
            logger.debug("Force flushing batch with {} items", currentBatch.size());
            
            List<TelemetryData> batchToFlush = new ArrayList<>(currentBatch);
            currentBatch.clear();
            
            processTelemetryBatch(batchToFlush);
            totalProcessed.addAndGet(batchToFlush.size());
            
            lastFlushTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Error flushing current batch", e);
            totalErrors.incrementAndGet();
        } finally {
            batchLock.unlock();
        }
    }
    
    /**
     * Process a batch of telemetry data
     */
    private void processTelemetryBatch(List<TelemetryData> batch) {
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            List<Point> points = new ArrayList<>();
            List<Point> spatialPoints = new ArrayList<>();
            
            for (TelemetryData data : batch) {
                // Create time-series point
                Point timeSeriesPoint = createTimeSeriesPoint(data);
                points.add(timeSeriesPoint);
                
                // Create spatial point if location data exists
                if (data.hasSpatialData()) {
                    Point spatialPoint = createSpatialPoint(data);
                    spatialPoints.add(spatialPoint);
                }
            }
            
            // Write time-series data
            if (!points.isEmpty()) {
                writeApi.writePoints(bucket, organization, points);
                logger.debug("Wrote {} time-series points to InfluxDB", points.size());
            }
            
            // Write spatial data
            if (!spatialPoints.isEmpty()) {
                writeApi.writePoints(bucket, organization, spatialPoints);
                logger.debug("Wrote {} spatial points to InfluxDB", spatialPoints.size());
            }
            
        } catch (Exception e) {
            logger.error("Error writing batch to InfluxDB", e);
            throw e;
        }
    }
    
    /**
     * Create time-series point from telemetry data
     */
    private Point createTimeSeriesPoint(TelemetryData data) {
        Point point = Point.measurement("telemetry")
            .time(data.getTimestamp(), WritePrecision.MS)
            .addTag("device_id", data.getDeviceId().getValue())
            .addTag("organization_id", data.getOrganizationId());
        
        // Add metrics as fields
        data.getMetrics().forEach((key, value) -> {
            if (value.isNumeric()) {
                point.addField(key, value.getNumericValue());
            } else if (value.isString()) {
                point.addField(key, value.getStringValue());
            } else if (value.isBoolean()) {
                point.addField(key, value.getBooleanValue());
            }
        });
        
        return point;
    }
    
    /**
     * Create spatial point from telemetry data
     */
    private Point createSpatialPoint(TelemetryData data) {
        Point point = Point.measurement("spatial_telemetry")
            .time(data.getTimestamp(), WritePrecision.MS)
            .addTag("device_id", data.getDeviceId().getValue())
            .addTag("organization_id", data.getOrganizationId())
            .addField("latitude", data.getLocation().latitude())
            .addField("longitude", data.getLocation().longitude());
        
        // Add numeric metrics as fields
        data.getMetrics().forEach((key, value) -> {
            if (value.isNumeric()) {
                point.addField(key, value.getNumericValue());
            }
        });
        
        return point;
    }
    
    /**
     * Handle write errors with retry logic
     */
    private void handleWriteError(WriteErrorEvent event) {
        // For now, just log the error
        // In production, you might want to implement retry logic or dead letter queue
        logger.error("Write error details: {}", event.getThrowable().getMessage());
    }
    
    /**
     * Get current batch processing statistics
     */
    public BatchProcessingStats getStats() {
        return new BatchProcessingStats(
            totalProcessed.get(),
            totalErrors.get(),
            batchesProcessed.get(),
            queueSize.get(),
            queueCapacity,
            System.currentTimeMillis() - lastFlushTime,
            calculateThroughput()
        );
    }
    
    /**
     * Calculate current throughput (points per second)
     */
    private double calculateThroughput() {
        long processed = totalProcessed.get();
        if (processed == 0) {
            return 0.0;
        }
        
        // Simple throughput calculation over last minute
        return processed / 60.0; // Approximate
    }
    
    /**
     * Check if batch processor is healthy
     */
    public boolean isHealthy() {
        // Consider healthy if queue is not full and error rate is low
        double errorRate = totalProcessed.get() > 0 ? 
            (double) totalErrors.get() / totalProcessed.get() : 0.0;
        
        return queueSize.get() < queueCapacity * 0.9 && errorRate < 0.05;
    }
    
    /**
     * Batch processing statistics
     */
    public record BatchProcessingStats(
        long totalProcessed,
        long totalErrors,
        long batchesProcessed,
        long currentQueueSize,
        int maxQueueCapacity,
        long timeSinceLastFlush,
        double throughputPerSecond
    ) {}
}