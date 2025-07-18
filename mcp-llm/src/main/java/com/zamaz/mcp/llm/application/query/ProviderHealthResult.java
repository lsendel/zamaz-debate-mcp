package com.zamaz.mcp.llm.application.query;

import com.zamaz.mcp.llm.domain.model.LlmModel;
import com.zamaz.mcp.llm.domain.model.ProviderStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result DTO for provider health check operations.
 * Contains current status, health metrics, and diagnostic information.
 */
public record ProviderHealthResult(
    String providerId,
    String providerName,
    ProviderStatus status,
    boolean isHealthy,
    Instant lastChecked,
    Duration responseTime,
    Optional<String> errorMessage,
    List<ModelHealthInfo> modelHealth,
    HealthMetrics metrics,
    Map<String, Object> diagnostics
) {
    
    public ProviderHealthResult {
        Objects.requireNonNull(providerId, "Provider ID cannot be null");
        Objects.requireNonNull(providerName, "Provider name cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(lastChecked, "Last checked cannot be null");
        Objects.requireNonNull(responseTime, "Response time cannot be null");
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        Objects.requireNonNull(modelHealth, "Model health cannot be null");
        Objects.requireNonNull(metrics, "Metrics cannot be null");
        Objects.requireNonNull(diagnostics, "Diagnostics cannot be null");
        
        if (providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be empty");
        }
        
        if (providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be empty");
        }
        
        if (responseTime.isNegative()) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
        
        if (!isHealthy && status.isHealthy()) {
            throw new IllegalArgumentException("Healthy status inconsistent with isHealthy flag");
        }
    }
    
    /**
     * Creates a basic healthy provider result.
     * 
     * @param providerId the provider identifier
     * @param providerName the provider name
     * @param responseTime the health check response time
     * @return a new ProviderHealthResult indicating health
     */
    public static ProviderHealthResult healthy(
            String providerId,
            String providerName,
            Duration responseTime
    ) {
        return new ProviderHealthResult(
            providerId,
            providerName,
            ProviderStatus.AVAILABLE,
            true,
            Instant.now(),
            responseTime,
            Optional.empty(),
            List.of(),
            HealthMetrics.empty(),
            Map.of("status", "healthy")
        );
    }
    
    /**
     * Creates an unhealthy provider result with error message.
     * 
     * @param providerId the provider identifier
     * @param providerName the provider name
     * @param status the provider status
     * @param errorMessage the error that caused the health issue
     * @param responseTime the health check response time
     * @return a new ProviderHealthResult indicating health issues
     */
    public static ProviderHealthResult unhealthy(
            String providerId,
            String providerName,
            ProviderStatus status,
            String errorMessage,
            Duration responseTime
    ) {
        return new ProviderHealthResult(
            providerId,
            providerName,
            status,
            false,
            Instant.now(),
            responseTime,
            Optional.of(errorMessage),
            List.of(),
            HealthMetrics.empty(),
            Map.of(
                "status", "unhealthy",
                "error", errorMessage
            )
        );
    }
    
    /**
     * Creates a degraded provider result.
     * 
     * @param providerId the provider identifier
     * @param providerName the provider name
     * @param responseTime the health check response time
     * @param warningMessage optional warning about degraded performance
     * @return a new ProviderHealthResult indicating degraded performance
     */
    public static ProviderHealthResult degraded(
            String providerId,
            String providerName,
            Duration responseTime,
            String warningMessage
    ) {
        return new ProviderHealthResult(
            providerId,
            providerName,
            ProviderStatus.DEGRADED,
            true, // Still considered healthy but degraded
            Instant.now(),
            responseTime,
            Optional.of(warningMessage),
            List.of(),
            HealthMetrics.empty(),
            Map.of(
                "status", "degraded",
                "warning", warningMessage
            )
        );
    }
    
    /**
     * Creates a comprehensive health result with model information.
     * 
     * @param providerId the provider identifier
     * @param providerName the provider name
     * @param status the provider status
     * @param responseTime the health check response time
     * @param modelHealth health information for individual models
     * @param metrics overall health metrics
     * @return a new ProviderHealthResult with detailed information
     */
    public static ProviderHealthResult comprehensive(
            String providerId,
            String providerName,
            ProviderStatus status,
            Duration responseTime,
            List<ModelHealthInfo> modelHealth,
            HealthMetrics metrics
    ) {
        boolean isHealthy = status.isHealthy() && 
                           modelHealth.stream().allMatch(ModelHealthInfo::isAvailable);
        
        return new ProviderHealthResult(
            providerId,
            providerName,
            status,
            isHealthy,
            Instant.now(),
            responseTime,
            Optional.empty(),
            List.copyOf(modelHealth),
            metrics,
            Map.of("check_type", "comprehensive")
        );
    }
    
    /**
     * Creates a rate-limited provider result.
     * 
     * @param providerId the provider identifier
     * @param providerName the provider name
     * @param responseTime the health check response time
     * @param retryAfter when to retry the health check
     * @return a new ProviderHealthResult indicating rate limiting
     */
    public static ProviderHealthResult rateLimited(
            String providerId,
            String providerName,
            Duration responseTime,
            Duration retryAfter
    ) {
        return new ProviderHealthResult(
            providerId,
            providerName,
            ProviderStatus.RATE_LIMITED,
            false,
            Instant.now(),
            responseTime,
            Optional.of("Provider is rate limiting requests"),
            List.of(),
            HealthMetrics.empty(),
            Map.of(
                "status", "rate_limited",
                "retry_after_seconds", retryAfter.getSeconds()
            )
        );
    }
    
    /**
     * Gets the health check age.
     * 
     * @return how long ago the health check was performed
     */
    public Duration getHealthCheckAge() {
        return Duration.between(lastChecked, Instant.now());
    }
    
    /**
     * Checks if the health check is stale.
     * 
     * @param maxAge the maximum allowed age
     * @return true if the health check is older than maxAge
     */
    public boolean isStale(Duration maxAge) {
        return getHealthCheckAge().compareTo(maxAge) > 0;
    }
    
    /**
     * Gets the number of healthy models.
     * 
     * @return count of healthy models
     */
    public long getHealthyModelCount() {
        return modelHealth.stream()
            .filter(ModelHealthInfo::isAvailable)
            .count();
    }
    
    /**
     * Gets the total number of models.
     * 
     * @return total model count
     */
    public int getTotalModelCount() {
        return modelHealth.size();
    }
    
    /**
     * Checks if this is a fast health check response.
     * 
     * @param threshold the response time threshold
     * @return true if response time is below threshold
     */
    public boolean isFastResponse(Duration threshold) {
        return responseTime.compareTo(threshold) <= 0;
    }
    
    /**
     * Nested record representing individual model health information.
     */
    public record ModelHealthInfo(
        String modelName,
        String displayName,
        LlmModel.ModelStatus status,
        boolean isAvailable,
        Optional<String> errorMessage,
        Duration lastTestResponseTime,
        Instant lastTested
    ) {
        
        public ModelHealthInfo {
            Objects.requireNonNull(modelName, "Model name cannot be null");
            Objects.requireNonNull(displayName, "Display name cannot be null");
            Objects.requireNonNull(status, "Status cannot be null");
            Objects.requireNonNull(errorMessage, "Error message cannot be null");
            Objects.requireNonNull(lastTestResponseTime, "Last test response time cannot be null");
            Objects.requireNonNull(lastTested, "Last tested cannot be null");
        }
        
        /**
         * Creates healthy model info.
         */
        public static ModelHealthInfo healthy(String modelName, String displayName, Duration responseTime) {
            return new ModelHealthInfo(
                modelName,
                displayName,
                LlmModel.ModelStatus.AVAILABLE,
                true,
                Optional.empty(),
                responseTime,
                Instant.now()
            );
        }
        
        /**
         * Creates unhealthy model info.
         */
        public static ModelHealthInfo unhealthy(String modelName, String displayName, String error) {
            return new ModelHealthInfo(
                modelName,
                displayName,
                LlmModel.ModelStatus.UNAVAILABLE,
                false,
                Optional.of(error),
                Duration.ZERO,
                Instant.now()
            );
        }
    }
    
    /**
     * Nested record representing health metrics.
     */
    public record HealthMetrics(
        double uptimePercentage,
        long totalHealthChecks,
        long successfulHealthChecks,
        long failedHealthChecks,
        Duration averageResponseTime,
        Instant firstChecked
    ) {
        
        public HealthMetrics {
            if (uptimePercentage < 0.0 || uptimePercentage > 100.0) {
                throw new IllegalArgumentException("Uptime percentage must be between 0.0 and 100.0");
            }
            if (totalHealthChecks < 0) {
                throw new IllegalArgumentException("Total health checks cannot be negative");
            }
            if (successfulHealthChecks < 0) {
                throw new IllegalArgumentException("Successful health checks cannot be negative");
            }
            if (failedHealthChecks < 0) {
                throw new IllegalArgumentException("Failed health checks cannot be negative");
            }
            if (averageResponseTime.isNegative()) {
                throw new IllegalArgumentException("Average response time cannot be negative");
            }
        }
        
        /**
         * Creates empty metrics.
         */
        public static HealthMetrics empty() {
            return new HealthMetrics(0.0, 0, 0, 0, Duration.ZERO, null);
        }
        
        /**
         * Creates basic metrics.
         */
        public static HealthMetrics of(long total, long successful, Duration avgResponseTime) {
            double uptime = total > 0 ? (double) successful / total * 100.0 : 0.0;
            return new HealthMetrics(
                uptime,
                total,
                successful,
                total - successful,
                avgResponseTime,
                Instant.now()
            );
        }
        
        /**
         * Gets the failure rate as a percentage.
         */
        public double getFailureRatePercentage() {
            return 100.0 - uptimePercentage;
        }
    }
}