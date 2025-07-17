package com.zamaz.mcp.github.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive health check service for monitoring all critical dependencies.
 * This service provides detailed health information about database, Redis, 
 * GitHub API, and other external services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService implements HealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MetricsService metricsService;
    private final SLOMonitoringService sloMonitoringService;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Main health check method called by Spring Boot Actuator
     */
    @Override
    public Health health() {
        try {
            Map<String, Object> healthDetails = new HashMap<>();
            boolean overallHealthy = true;
            
            // Check database health
            Health.Builder dbHealth = checkDatabaseHealth();
            healthDetails.put("database", dbHealth.build().getDetails());
            if (dbHealth.build().getStatus() != org.springframework.boot.actuator.health.Status.UP) {
                overallHealthy = false;
            }
            
            // Check Redis health
            Health.Builder redisHealth = checkRedisHealth();
            healthDetails.put("redis", redisHealth.build().getDetails());
            if (redisHealth.build().getStatus() != org.springframework.boot.actuator.health.Status.UP) {
                overallHealthy = false;
            }
            
            // Check GitHub API health
            Health.Builder githubHealth = checkGitHubApiHealth();
            healthDetails.put("github_api", githubHealth.build().getDetails());
            if (githubHealth.build().getStatus() != org.springframework.boot.actuator.health.Status.UP) {
                overallHealthy = false;
            }
            
            // Check external services
            Health.Builder externalHealth = checkExternalServices();
            healthDetails.put("external_services", externalHealth.build().getDetails());
            if (externalHealth.build().getStatus() != org.springframework.boot.actuator.health.Status.UP) {
                overallHealthy = false;
            }
            
            // Check SLO compliance
            Health.Builder sloHealth = checkSLOCompliance();
            healthDetails.put("slo_compliance", sloHealth.build().getDetails());
            
            // Record health metrics
            recordHealthMetrics(healthDetails);
            
            // Update uptime/downtime for SLO calculation
            if (overallHealthy) {
                sloMonitoringService.recordUptime(1);
            } else {
                sloMonitoringService.recordDowntime(1);
            }
            
            return overallHealthy ? 
                    Health.up().withDetails(healthDetails).build() :
                    Health.down().withDetails(healthDetails).build();
                    
        } catch (Exception e) {
            log.error("Error performing health check", e);
            sloMonitoringService.recordDowntime(1);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Checks database connectivity and performance
     */
    private Health.Builder checkDatabaseHealth() {
        Health.Builder health = Health.up();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test database connection
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
                long responseTime = System.currentTimeMillis() - startTime;
                
                health.withDetail("connection_valid", isValid)
                      .withDetail("response_time_ms", responseTime)
                      .withDetail("connection_url", connection.getMetaData().getURL());
                
                if (!isValid || responseTime > 5000) {
                    health = Health.down()
                            .withDetail("reason", "Database connection invalid or slow")
                            .withDetail("response_time_ms", responseTime);
                }
                
                // Record database connection metrics
                metricsService.recordDatabaseQuery("health_check", "connection_test", 
                        Duration.ofMillis(responseTime));
                
            }
        } catch (SQLException e) {
            log.error("Database health check failed", e);
            health = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("error_code", e.getErrorCode());
        }
        
        return health;
    }

    /**
     * Checks Redis connectivity and performance
     */
    private Health.Builder checkRedisHealth() {
        Health.Builder health = Health.up();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test Redis connection with ping
            String pingResponse = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.withDetail("ping_response", pingResponse)
                  .withDetail("response_time_ms", responseTime);
            
            if (!"PONG".equals(pingResponse) || responseTime > 1000) {
                health = Health.down()
                        .withDetail("reason", "Redis ping failed or slow")
                        .withDetail("response_time_ms", responseTime);
            }
            
            // Test Redis operations
            try {
                redisTemplate.opsForValue().set("health_check", "test", 1, TimeUnit.SECONDS);
                String value = (String) redisTemplate.opsForValue().get("health_check");
                
                health.withDetail("operations_test", "test".equals(value) ? "passed" : "failed");
                
                // Record Redis operation metrics
                metricsService.recordRedisOperation("health_check", "test".equals(value), 
                        Duration.ofMillis(responseTime));
                
            } catch (Exception e) {
                health = Health.down()
                        .withDetail("reason", "Redis operations failed")
                        .withDetail("error", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            health = Health.down()
                    .withDetail("error", e.getMessage());
        }
        
        return health;
    }

    /**
     * Checks GitHub API availability and rate limits
     */
    private Health.Builder checkGitHubApiHealth() {
        Health.Builder health = Health.up();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test GitHub API with rate limit endpoint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/rate_limit"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.withDetail("status_code", response.statusCode())
                  .withDetail("response_time_ms", responseTime);
            
            if (response.statusCode() != 200) {
                health = Health.down()
                        .withDetail("reason", "GitHub API returned non-200 status")
                        .withDetail("status_code", response.statusCode())
                        .withDetail("response_body", response.body());
            } else {
                // Parse rate limit information
                String responseBody = response.body();
                health.withDetail("rate_limit_info", responseBody);
            }
            
            // Record GitHub API metrics
            metricsService.recordGitHubApiCall("rate_limit", "GET");
            metricsService.recordGitHubApiResponse("rate_limit", "GET", 
                    Duration.ofMillis(responseTime), response.statusCode());
            
        } catch (IOException | InterruptedException e) {
            log.error("GitHub API health check failed", e);
            health = Health.down()
                    .withDetail("error", e.getMessage());
            
            metricsService.recordGitHubApiError("rate_limit", "GET", e.getClass().getSimpleName());
        }
        
        return health;
    }

    /**
     * Checks external services (Kiro API, etc.)
     */
    private Health.Builder checkExternalServices() {
        Health.Builder health = Health.up();
        Map<String, Object> servicesStatus = new HashMap<>();
        
        // Check Kiro API health
        try {
            CompletableFuture<HttpResponse<String>> kiroHealthCheck = 
                    checkExternalServiceAsync("http://localhost:5013/health", "kiro_api");
            
            HttpResponse<String> kiroResponse = kiroHealthCheck.get(5, TimeUnit.SECONDS);
            
            servicesStatus.put("kiro_api", Map.of(
                    "status_code", kiroResponse.statusCode(),
                    "healthy", kiroResponse.statusCode() == 200
            ));
            
        } catch (Exception e) {
            log.warn("Kiro API health check failed", e);
            servicesStatus.put("kiro_api", Map.of(
                    "healthy", false,
                    "error", e.getMessage()
            ));
        }
        
        // Add more external service checks here
        
        health.withDetail("services", servicesStatus);
        
        return health;
    }

    /**
     * Checks SLO compliance
     */
    private Health.Builder checkSLOCompliance() {
        Health.Builder health = Health.up();
        
        try {
            SLOMonitoringService.SLOStatus sloStatus = sloMonitoringService.getCurrentSLOStatus();
            
            health.withDetail("overall_compliance", sloStatus.getOverallCompliance())
                  .withDetail("pr_processing_p95_compliance", sloStatus.getPrProcessingP95Compliance())
                  .withDetail("error_rate_compliance", sloStatus.getErrorRateCompliance())
                  .withDetail("availability_compliance", sloStatus.getAvailabilityCompliance())
                  .withDetail("github_api_p95_compliance", sloStatus.getGithubApiP95Compliance())
                  .withDetail("last_calculated", sloStatus.getLastCalculated());
            
            // Mark as degraded if overall compliance is below threshold
            if (sloStatus.getOverallCompliance() < 0.95) {
                health = Health.down()
                        .withDetail("reason", "SLO compliance below threshold")
                        .withDetail("overall_compliance", sloStatus.getOverallCompliance());
            }
            
        } catch (Exception e) {
            log.error("SLO compliance check failed", e);
            health.withDetail("slo_check_error", e.getMessage());
        }
        
        return health;
    }

    /**
     * Records health metrics
     */
    private void recordHealthMetrics(Map<String, Object> healthDetails) {
        try {
            // Record database health
            Object dbHealth = healthDetails.get("database");
            if (dbHealth instanceof Map) {
                Map<String, Object> dbDetails = (Map<String, Object>) dbHealth;
                Boolean dbHealthy = (Boolean) dbDetails.get("connection_valid");
                metricsService.recordSLOMetric("database_health", dbHealthy != null && dbHealthy ? 1.0 : 0.0);
            }
            
            // Record Redis health
            Object redisHealth = healthDetails.get("redis");
            if (redisHealth instanceof Map) {
                Map<String, Object> redisDetails = (Map<String, Object>) redisHealth;
                String pingResponse = (String) redisDetails.get("ping_response");
                metricsService.recordSLOMetric("redis_health", "PONG".equals(pingResponse) ? 1.0 : 0.0);
            }
            
            // Record GitHub API health
            Object githubHealth = healthDetails.get("github_api");
            if (githubHealth instanceof Map) {
                Map<String, Object> githubDetails = (Map<String, Object>) githubHealth;
                Integer statusCode = (Integer) githubDetails.get("status_code");
                metricsService.recordSLOMetric("github_api_health", 
                        statusCode != null && statusCode == 200 ? 1.0 : 0.0);
            }
            
        } catch (Exception e) {
            log.error("Error recording health metrics", e);
        }
    }

    /**
     * Async health check for external services
     */
    private CompletableFuture<HttpResponse<String>> checkExternalServiceAsync(String url, String serviceName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Detailed health check for specific components
     */
    public Map<String, Object> getDetailedHealthCheck() {
        Map<String, Object> detailedHealth = new HashMap<>();
        
        // Get current health status
        Health currentHealth = health();
        detailedHealth.put("overall_status", currentHealth.getStatus().getCode());
        detailedHealth.put("details", currentHealth.getDetails());
        
        // Add system information
        Runtime runtime = Runtime.getRuntime();
        detailedHealth.put("system_info", Map.of(
                "available_processors", runtime.availableProcessors(),
                "free_memory", runtime.freeMemory(),
                "total_memory", runtime.totalMemory(),
                "max_memory", runtime.maxMemory()
        ));
        
        // Add JVM information
        detailedHealth.put("jvm_info", Map.of(
                "java_version", System.getProperty("java.version"),
                "jvm_name", System.getProperty("java.vm.name"),
                "jvm_vendor", System.getProperty("java.vm.vendor")
        ));
        
        return detailedHealth;
    }
}