package com.example.workflow.infrastructure.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

/**
 * InfluxDB configuration for telemetry data storage
 * Configures connection, client options, and performance settings
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "influxdb.enabled", havingValue = "true", matchIfMissing = true)
public class InfluxDbConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbConfig.class);
    
    @Value("${influxdb.url:http://localhost:8086}")
    private String url;
    
    @Value("${influxdb.token:}")
    private String token;
    
    @Value("${influxdb.username:admin}")
    private String username;
    
    @Value("${influxdb.password:password}")
    private String password;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.organization:workflow-org}")
    private String organization;
    
    @Value("${influxdb.connection.timeout:10000}")
    private int connectionTimeout;
    
    @Value("${influxdb.connection.read-timeout:30000}")
    private int readTimeout;
    
    @Value("${influxdb.connection.write-timeout:10000}")
    private int writeTimeout;
    
    @Value("${influxdb.batch.size:1000}")
    private int batchSize;
    
    @Value("${influxdb.batch.flush-interval:1000}")
    private int flushInterval;
    
    /**
     * Create InfluxDB client with optimized settings for high-frequency telemetry data
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        logger.info("Configuring InfluxDB client for URL: {}, Organization: {}, Bucket: {}", 
            url, organization, bucket);
        
        try {
            InfluxDBClientOptions.Builder optionsBuilder = InfluxDBClientOptions.builder()
                .url(url)
                .org(organization)
                .bucket(bucket)
                .connectTimeout(Duration.ofMillis(connectionTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .writeTimeout(Duration.ofMillis(writeTimeout));
            
            // Configure authentication
            if (token != null && !token.trim().isEmpty()) {
                logger.info("Using token-based authentication");
                optionsBuilder.authenticateToken(token.toCharArray());
            } else if (username != null && !username.trim().isEmpty()) {
                logger.info("Using username/password authentication for user: {}", username);
                optionsBuilder.authenticate(username, password.toCharArray());
            } else {
                logger.warn("No authentication configured for InfluxDB");
            }
            
            InfluxDBClientOptions options = optionsBuilder.build();
            InfluxDBClient client = InfluxDBClientFactory.create(options);
            
            // Test connection
            testConnection(client);
            
            logger.info("InfluxDB client configured successfully");
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to configure InfluxDB client", e);
            throw new RuntimeException("InfluxDB configuration failed", e);
        }
    }
    
    /**
     * Test InfluxDB connection
     */
    private void testConnection(InfluxDBClient client) {
        try {
            // Simple ping to test connectivity
            boolean isReady = client.ready();
            if (isReady) {
                logger.info("InfluxDB connection test successful");
            } else {
                logger.warn("InfluxDB is not ready");
            }
            
            // Test basic query
            String testQuery = "buckets() |> limit(n: 1)";
            client.getQueryApi().query(testQuery, organization);
            
            logger.info("InfluxDB query test successful");
            
        } catch (Exception e) {
            logger.error("InfluxDB connection test failed", e);
            // Don't throw exception here to allow application to start
            // The health check will detect the issue
        }
    }
    
    /**
     * Configuration properties for other components
     */
    @Bean
    public InfluxDbProperties influxDbProperties() {
        return new InfluxDbProperties(
            url, token, username, password, bucket, organization,
            connectionTimeout, readTimeout, writeTimeout,
            batchSize, flushInterval
        );
    }
    
    /**
     * InfluxDB configuration properties
     */
    public record InfluxDbProperties(
        String url,
        String token,
        String username,
        String password,
        String bucket,
        String organization,
        int connectionTimeout,
        int readTimeout,
        int writeTimeout,
        int batchSize,
        int flushInterval
    ) {}
}