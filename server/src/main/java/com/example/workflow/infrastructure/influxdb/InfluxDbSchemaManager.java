package com.example.workflow.infrastructure.influxdb;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import com.influxdb.client.domain.Organization;
import com.influxdb.client.domain.Task;
import com.influxdb.client.TasksApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

/**
 * Manages InfluxDB schema setup including buckets, retention policies, and downsampling tasks
 */
@Component
public class InfluxDbSchemaManager {
    
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbSchemaManager.class);
    
    private final InfluxDBClient influxDBClient;
    private final BucketsApi bucketsApi;
    private final TasksApi tasksApi;
    
    @Value("${influxdb.bucket:telemetry}")
    private String primaryBucket;
    
    @Value("${influxdb.organization:workflow-org}")
    private String organizationName;
    
    @Value("${influxdb.retention.raw-data-days:30}")
    private int rawDataRetentionDays;
    
    @Value("${influxdb.retention.aggregated-data-days:365}")
    private int aggregatedDataRetentionDays;
    
    @Autowired
    public InfluxDbSchemaManager(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
        this.bucketsApi = influxDBClient.getBucketsApi();
        this.tasksApi = influxDBClient.getTasksApi();
    }
    
    @PostConstruct
    public void initializeSchema() {
        try {
            logger.info("Initializing InfluxDB schema...");
            
            Organization organization = getOrCreateOrganization();
            
            // Create primary telemetry bucket with retention policy
            createTelemetryBucket(organization);
            
            // Create aggregated data bucket
            createAggregatedBucket(organization);
            
            // Create downsampling tasks
            createDownsamplingTasks(organization);
            
            logger.info("InfluxDB schema initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize InfluxDB schema", e);
            throw new RuntimeException("InfluxDB schema initialization failed", e);
        }
    }
    
    /**
     * Get or create organization
     */
    private Organization getOrCreateOrganization() {
        try {
            List<Organization> organizations = influxDBClient.getOrganizationsApi().findOrganizations();
            return organizations.stream()
                .filter(org -> organizationName.equals(org.getName()))
                .findFirst()
                .orElseGet(() -> {
                    logger.info("Creating organization: {}", organizationName);
                    return influxDBClient.getOrganizationsApi().createOrganization(organizationName);
                });
        } catch (Exception e) {
            logger.warn("Could not create organization, using existing: {}", e.getMessage());
            // Return a default organization if creation fails
            return new Organization().name(organizationName);
        }
    }
    
    /**
     * Create primary telemetry bucket with retention policy
     */
    private void createTelemetryBucket(Organization organization) {
        String bucketName = primaryBucket;
        
        Bucket existingBucket = bucketsApi.findBucketByName(bucketName);
        if (existingBucket != null) {
            logger.info("Telemetry bucket '{}' already exists", bucketName);
            updateBucketRetention(existingBucket, Duration.ofDays(rawDataRetentionDays));
            return;
        }
        
        logger.info("Creating telemetry bucket '{}' with {} days retention", bucketName, rawDataRetentionDays);
        
        BucketRetentionRules retentionRule = new BucketRetentionRules()
            .type(BucketRetentionRules.TypeEnum.EXPIRE)
            .everySeconds((long) rawDataRetentionDays * 24 * 60 * 60);
        
        Bucket bucket = new Bucket()
            .name(bucketName)
            .orgID(organization.getId())
            .retentionRules(List.of(retentionRule))
            .description("Primary telemetry data storage with " + rawDataRetentionDays + " days retention");
        
        bucketsApi.createBucket(bucket);
        logger.info("Created telemetry bucket: {}", bucketName);
    }
    
    /**
     * Create aggregated data bucket for downsampled data
     */
    private void createAggregatedBucket(Organization organization) {
        String bucketName = primaryBucket + "_aggregated";
        
        Bucket existingBucket = bucketsApi.findBucketByName(bucketName);
        if (existingBucket != null) {
            logger.info("Aggregated bucket '{}' already exists", bucketName);
            updateBucketRetention(existingBucket, Duration.ofDays(aggregatedDataRetentionDays));
            return;
        }
        
        logger.info("Creating aggregated bucket '{}' with {} days retention", bucketName, aggregatedDataRetentionDays);
        
        BucketRetentionRules retentionRule = new BucketRetentionRules()
            .type(BucketRetentionRules.TypeEnum.EXPIRE)
            .everySeconds((long) aggregatedDataRetentionDays * 24 * 60 * 60);
        
        Bucket bucket = new Bucket()
            .name(bucketName)
            .orgID(organization.getId())
            .retentionRules(List.of(retentionRule))
            .description("Aggregated telemetry data with " + aggregatedDataRetentionDays + " days retention");
        
        bucketsApi.createBucket(bucket);
        logger.info("Created aggregated bucket: {}", bucketName);
    }
    
    /**
     * Update bucket retention policy
     */
    private void updateBucketRetention(Bucket bucket, Duration retention) {
        try {
            BucketRetentionRules retentionRule = new BucketRetentionRules()
                .type(BucketRetentionRules.TypeEnum.EXPIRE)
                .everySeconds(retention.getSeconds());
            
            bucket.setRetentionRules(List.of(retentionRule));
            bucketsApi.updateBucket(bucket);
            
            logger.info("Updated retention policy for bucket '{}' to {} days", 
                bucket.getName(), retention.toDays());
        } catch (Exception e) {
            logger.warn("Failed to update retention policy for bucket '{}': {}", 
                bucket.getName(), e.getMessage());
        }
    }
    
    /**
     * Create downsampling tasks for data aggregation
     */
    private void createDownsamplingTasks(Organization organization) {
        createHourlyDownsamplingTask(organization);
        createDailyDownsamplingTask(organization);
    }
    
    /**
     * Create hourly downsampling task
     */
    private void createHourlyDownsamplingTask(Organization organization) {
        String taskName = "telemetry_hourly_downsample";
        
        // Check if task already exists
        List<Task> existingTasks = tasksApi.findTasks();
        boolean taskExists = existingTasks.stream()
            .anyMatch(task -> taskName.equals(task.getName()));
        
        if (taskExists) {
            logger.info("Hourly downsampling task already exists");
            return;
        }
        
        String fluxScript = String.format("""
            option task = {name: "%s", every: 1h}
            
            from(bucket: "%s")
              |> range(start: -2h, stop: -1h)
              |> filter(fn: (r) => r._measurement == "telemetry")
              |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)
              |> set(key: "_measurement", value: "telemetry_hourly")
              |> to(bucket: "%s_aggregated", org: "%s")
            """, taskName, primaryBucket, primaryBucket, organizationName);
        
        try {
            Task task = new Task()
                .name(taskName)
                .orgID(organization.getId())
                .flux(fluxScript)
                .description("Hourly downsampling of telemetry data")
                .status(Task.StatusEnum.ACTIVE);
            
            tasksApi.createTask(task);
            logger.info("Created hourly downsampling task");
        } catch (Exception e) {
            logger.warn("Failed to create hourly downsampling task: {}", e.getMessage());
        }
    }
    
    /**
     * Create daily downsampling task
     */
    private void createDailyDownsamplingTask(Organization organization) {
        String taskName = "telemetry_daily_downsample";
        
        // Check if task already exists
        List<Task> existingTasks = tasksApi.findTasks();
        boolean taskExists = existingTasks.stream()
            .anyMatch(task -> taskName.equals(task.getName()));
        
        if (taskExists) {
            logger.info("Daily downsampling task already exists");
            return;
        }
        
        String fluxScript = String.format("""
            option task = {name: "%s", every: 1d}
            
            from(bucket: "%s_aggregated")
              |> range(start: -2d, stop: -1d)
              |> filter(fn: (r) => r._measurement == "telemetry_hourly")
              |> aggregateWindow(every: 1d, fn: mean, createEmpty: false)
              |> set(key: "_measurement", value: "telemetry_daily")
              |> to(bucket: "%s_aggregated", org: "%s")
            """, taskName, primaryBucket, primaryBucket, organizationName);
        
        try {
            Task task = new Task()
                .name(taskName)
                .orgID(organization.getId())
                .flux(fluxScript)
                .description("Daily downsampling of hourly telemetry data")
                .status(Task.StatusEnum.ACTIVE);
            
            tasksApi.createTask(task);
            logger.info("Created daily downsampling task");
        } catch (Exception e) {
            logger.warn("Failed to create daily downsampling task: {}", e.getMessage());
        }
    }
    
    /**
     * Get measurement schema information
     */
    public MeasurementSchema getTelemetryMeasurementSchema() {
        return new MeasurementSchema(
            "telemetry",
            "Primary telemetry measurement for real-time sensor data",
            List.of("device_id", "organization_id"),
            List.of("temperature", "humidity", "motion", "air_quality", "status"),
            Duration.ofDays(rawDataRetentionDays)
        );
    }
    
    /**
     * Get spatial measurement schema information
     */
    public MeasurementSchema getSpatialMeasurementSchema() {
        return new MeasurementSchema(
            "spatial_telemetry",
            "Spatial telemetry measurement for location-based sensor data",
            List.of("device_id", "organization_id"),
            List.of("latitude", "longitude", "temperature", "humidity", "motion", "air_quality"),
            Duration.ofDays(rawDataRetentionDays)
        );
    }
    
    /**
     * Measurement schema information
     */
    public record MeasurementSchema(
        String name,
        String description,
        List<String> tags,
        List<String> fields,
        Duration retention
    ) {}
}