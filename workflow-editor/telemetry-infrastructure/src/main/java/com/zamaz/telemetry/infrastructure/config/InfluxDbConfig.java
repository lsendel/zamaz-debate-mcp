package com.zamaz.telemetry.infrastructure.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.domain.HealthCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;

@Configuration
@Slf4j
public class InfluxDbConfig {
    
    @Value("${influxdb.url:http://localhost:8086}")
    private String influxDbUrl;
    
    @Value("${influxdb.token}")
    private String influxDbToken;
    
    @Value("${influxdb.org:workflow-editor}")
    private String organization;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.timeout:30}")
    private int timeoutSeconds;
    
    private InfluxDBClient influxDBClient;
    
    @Bean
    public InfluxDBClient influxDBClient() {
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(influxDbUrl)
                .authenticateToken(influxDbToken.toCharArray())
                .org(organization)
                .bucket(bucket)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        
        influxDBClient = InfluxDBClientFactory.create(options);
        return influxDBClient;
    }
    
    @PostConstruct
    public void init() {
        try {
            HealthCheck health = influxDBClient.health();
            log.info("InfluxDB connection established. Status: {}, Version: {}", 
                    health.getStatus(), health.getVersion());
            
            // Create bucket if it doesn't exist
            createBucketIfNotExists();
        } catch (Exception e) {
            log.error("Failed to connect to InfluxDB", e);
        }
    }
    
    private void createBucketIfNotExists() {
        try {
            var bucketsApi = influxDBClient.getBucketsApi();
            var existingBucket = bucketsApi.findBucketByName(bucket);
            
            if (existingBucket == null) {
                var orgApi = influxDBClient.getOrganizationsApi();
                var org = orgApi.findOrganizations().stream()
                        .filter(o -> o.getName().equals(organization))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Organization not found: " + organization));
                
                bucketsApi.createBucket(bucket, org);
                log.info("Created InfluxDB bucket: {}", bucket);
            } else {
                log.info("InfluxDB bucket already exists: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not create bucket, it may already exist: {}", e.getMessage());
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (influxDBClient != null) {
            influxDBClient.close();
            log.info("InfluxDB connection closed");
        }
    }
}