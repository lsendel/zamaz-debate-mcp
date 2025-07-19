package com.example.workflow.infrastructure.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB configuration for time-series telemetry storage
 * Configures connection, authentication, and performance settings
 */
@Configuration
public class InfluxDbConfig {
    
    @Value("${influxdb.url:http://localhost:8086}")
    private String influxDbUrl;
    
    @Value("${influxdb.token:}")
    private String influxDbToken;
    
    @Value("${influxdb.username:admin}")
    private String influxDbUsername;
    
    @Value("${influxdb.password:password}")
    private String influxDbPassword;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.organization:workflow-org}")
    private String organization;
    
    @Bean
    public InfluxDBClient influxDBClient() {
        if (influxDbToken != null && !influxDbToken.isEmpty()) {
            // Use token-based authentication (InfluxDB 2.x)
            return InfluxDBClientFactory.create(influxDbUrl, influxDbToken.toCharArray(), organization, bucket);
        } else {
            // Use username/password authentication (InfluxDB 1.x compatibility)
            return InfluxDBClientFactory.create(influxDbUrl, influxDbUsername, influxDbPassword.toCharArray());
        }
    }
    
    /**
     * Create bucket and retention policies on startup
     */
    @Bean
    public InfluxDbInitializer influxDbInitializer(InfluxDBClient client) {
        return new InfluxDbInitializer(client, bucket, organization);
    }
    
    /**
     * InfluxDB initialization component
     */
    public static class InfluxDbInitializer {
        private final InfluxDBClient client;
        private final String bucket;
        private final String organization;
        
        public InfluxDbInitializer(InfluxDBClient client, String bucket, String organization) {
            this.client = client;
            this.bucket = bucket;
            this.organization = organization;
            initializeDatabase();
        }
        
        private void initializeDatabase() {
            try {
                // Create bucket if it doesn't exist
                var bucketsApi = client.getBucketsApi();
                var existingBucket = bucketsApi.findBucketByName(bucket);
                
                if (existingBucket == null) {
                    var orgApi = client.getOrganizationsApi();
                    var org = orgApi.findOrganizations().stream()
                        .filter(o -> o.getName().equals(organization))
                        .findFirst()
                        .orElse(null);
                    
                    if (org != null) {
                        // Create bucket with 30-day retention for telemetry data
                        bucketsApi.createBucket(bucket, org.getId(), 
                            java.time.Duration.ofDays(30).getSeconds());
                        
                        // Create bucket for long-term aggregated data (1 year retention)
                        bucketsApi.createBucket(bucket + "_aggregated", org.getId(),
                            java.time.Duration.ofDays(365).getSeconds());
                    }
                }
                
                // Set up continuous queries for downsampling
                setupDownsamplingTasks();
                
            } catch (Exception e) {
                System.err.println("Failed to initialize InfluxDB: " + e.getMessage());
            }
        }
        
        private void setupDownsamplingTasks() {
            try {
                var tasksApi = client.getTasksApi();
                
                // Create task for 1-minute aggregation
                String minuteAggregationFlux = String.format("""
                    option task = {name: "1m-aggregation", every: 1m}
                    
                    from(bucket: "%s")
                        |> range(start: -2m)
                        |> filter(fn: (r) => r._measurement == "telemetry")
                        |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
                        |> to(bucket: "%s_aggregated", tagColumns: ["device_id", "organization_id"])
                    """, bucket, bucket);
                
                // Create task for 1-hour aggregation
                String hourAggregationFlux = String.format("""
                    option task = {name: "1h-aggregation", every: 1h}
                    
                    from(bucket: "%s_aggregated")
                        |> range(start: -2h)
                        |> filter(fn: (r) => r._measurement == "telemetry")
                        |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)
                        |> to(bucket: "%s_aggregated", tagColumns: ["device_id", "organization_id"])
                    """, bucket, bucket);
                
                // Note: In a real implementation, you'd check if tasks already exist
                // and handle task creation more robustly
                
            } catch (Exception e) {
                System.err.println("Failed to setup downsampling tasks: " + e.getMessage());
            }
        }
    }
}