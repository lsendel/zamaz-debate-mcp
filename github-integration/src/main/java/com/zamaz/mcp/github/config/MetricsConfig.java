package com.zamaz.mcp.github.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for custom metrics related to GitHub integration and PR processing.
 * This class defines all business-specific metrics for monitoring PR processing performance,
 * GitHub API interactions, and system health.
 */
@Configuration
@Slf4j
public class MetricsConfig {

    // Atomic counters for real-time metrics
    private final AtomicInteger activePRProcessingCount = new AtomicInteger(0);
    private final AtomicInteger queuedPRCount = new AtomicInteger(0);
    private final AtomicLong totalPRsProcessed = new AtomicLong(0);
    private final AtomicLong failedPRsCount = new AtomicLong(0);
    private final AtomicInteger githubApiCalls = new AtomicInteger(0);
    private final AtomicInteger githubApiErrors = new AtomicInteger(0);
    private final AtomicInteger dbConnectionsActive = new AtomicInteger(0);
    private final AtomicInteger redisConnectionsActive = new AtomicInteger(0);

    /**
     * Custom metrics binder for GitHub integration specific metrics
     */
    @Bean
    public MeterBinder githubIntegrationMetrics(MeterRegistry meterRegistry) {
        return registry -> {
            // PR Processing Metrics
            createPRProcessingMetrics(registry);
            
            // GitHub API Metrics
            createGitHubApiMetrics(registry);
            
            // Database and Cache Metrics
            createDatabaseMetrics(registry);
            
            // Business Logic Metrics
            createBusinessMetrics(registry);
            
            // SLO Metrics
            createSLOMetrics(registry);
            
            log.info("Custom GitHub integration metrics registered successfully");
        };
    }

    /**
     * PR Processing related metrics
     */
    private void createPRProcessingMetrics(MeterRegistry registry) {
        // Active PR processing gauge
        Gauge.builder("github_integration_pr_processing_active")
                .description("Number of PRs currently being processed")
                .register(registry, activePRProcessingCount, AtomicInteger::get);

        // Queued PR count
        Gauge.builder("github_integration_pr_queue_size")
                .description("Number of PRs in processing queue")
                .register(registry, queuedPRCount, AtomicInteger::get);

        // Total PRs processed counter
        Gauge.builder("github_integration_pr_processed_total")
                .description("Total number of PRs processed since startup")
                .register(registry, totalPRsProcessed, AtomicLong::get);

        // Failed PRs counter
        Gauge.builder("github_integration_pr_failed_total")
                .description("Total number of PRs that failed processing")
                .register(registry, failedPRsCount, AtomicLong::get);

        // PR processing duration timer
        Timer.builder("github_integration_pr_processing_duration")
                .description("Time taken to process a PR")
                .minimumExpectedValue(Duration.ofMillis(100))
                .maximumExpectedValue(Duration.ofMinutes(5))
                .publishPercentileHistogram()
                .register(registry);

        // Review generation duration
        Timer.builder("github_integration_review_generation_duration")
                .description("Time taken to generate review comments")
                .minimumExpectedValue(Duration.ofMillis(50))
                .maximumExpectedValue(Duration.ofMinutes(2))
                .publishPercentileHistogram()
                .register(registry);

        // File analysis duration
        Timer.builder("github_integration_file_analysis_duration")
                .description("Time taken to analyze individual files")
                .minimumExpectedValue(Duration.ofMillis(10))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .publishPercentileHistogram()
                .register(registry);
    }

    /**
     * GitHub API related metrics
     */
    private void createGitHubApiMetrics(MeterRegistry registry) {
        // GitHub API calls counter
        Counter.builder("github_integration_github_api_calls_total")
                .description("Total number of GitHub API calls")
                .register(registry);

        // GitHub API errors counter
        Counter.builder("github_integration_github_api_errors_total")
                .description("Total number of GitHub API errors")
                .register(registry);

        // GitHub API rate limit remaining
        Gauge.builder("github_integration_github_api_rate_limit_remaining")
                .description("Remaining GitHub API rate limit")
                .register(registry, this, config -> 0); // Will be updated by API client

        // GitHub API response time
        Timer.builder("github_integration_github_api_response_time")
                .description("GitHub API response time")
                .minimumExpectedValue(Duration.ofMillis(50))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .publishPercentileHistogram()
                .register(registry);

        // GitHub API retry attempts
        Counter.builder("github_integration_github_api_retries_total")
                .description("Total number of GitHub API retry attempts")
                .register(registry);
    }

    /**
     * Database and cache related metrics
     */
    private void createDatabaseMetrics(MeterRegistry registry) {
        // Database query duration
        Timer.builder("github_integration_database_query_duration")
                .description("Database query execution time")
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(5))
                .publishPercentileHistogram()
                .register(registry);

        // Database connection pool metrics
        Gauge.builder("github_integration_database_connections_active")
                .description("Number of active database connections")
                .register(registry, dbConnectionsActive, AtomicInteger::get);

        // Redis cache operations
        Counter.builder("github_integration_redis_operations_total")
                .description("Total number of Redis operations")
                .register(registry);

        // Cache hit ratio
        Gauge.builder("github_integration_cache_hit_ratio")
                .description("Cache hit ratio (0-1)")
                .register(registry, this, config -> 0.0); // Will be updated by cache service

        // Redis connection health
        Gauge.builder("github_integration_redis_connections_active")
                .description("Number of active Redis connections")
                .register(registry, redisConnectionsActive, AtomicInteger::get);
    }

    /**
     * Business logic related metrics
     */
    private void createBusinessMetrics(MeterRegistry registry) {
        // Review comments generated
        Counter.builder("github_integration_review_comments_generated_total")
                .description("Total number of review comments generated")
                .register(registry);

        // Issues detected
        Counter.builder("github_integration_issues_detected_total")
                .description("Total number of issues detected in PRs")
                .register(registry);

        // Auto-fixes applied
        Counter.builder("github_integration_auto_fixes_applied_total")
                .description("Total number of auto-fixes applied")
                .register(registry);

        // Webhook events processed
        Counter.builder("github_integration_webhook_events_processed_total")
                .description("Total number of webhook events processed")
                .register(registry);

        // Repository configurations
        Gauge.builder("github_integration_configured_repositories")
                .description("Number of repositories configured for integration")
                .register(registry, this, config -> 0); // Will be updated by repository service
    }

    /**
     * SLO (Service Level Objective) related metrics
     */
    private void createSLOMetrics(MeterRegistry registry) {
        // SLO: PR processing success rate
        Gauge.builder("github_integration_slo_pr_success_rate")
                .description("SLO: PR processing success rate (0-1)")
                .register(registry, this, config -> calculatePRSuccessRate());

        // SLO: P95 response time compliance
        Gauge.builder("github_integration_slo_p95_compliance")
                .description("SLO: P95 response time compliance (0-1)")
                .register(registry, this, config -> 0.0); // Will be calculated by SLO service

        // SLO: Availability
        Gauge.builder("github_integration_slo_availability")
                .description("SLO: Service availability (0-1)")
                .register(registry, this, config -> 1.0); // Will be calculated by health check service

        // SLO: Error rate
        Gauge.builder("github_integration_slo_error_rate")
                .description("SLO: Error rate (0-1)")
                .register(registry, this, config -> calculateErrorRate());
    }

    /**
     * Helper methods for metric calculations
     */
    private double calculatePRSuccessRate() {
        long total = totalPRsProcessed.get();
        long failed = failedPRsCount.get();
        return total > 0 ? (double) (total - failed) / total : 1.0;
    }

    private double calculateErrorRate() {
        long total = totalPRsProcessed.get();
        long failed = failedPRsCount.get();
        return total > 0 ? (double) failed / total : 0.0;
    }

    // Getters for updating metrics from other components
    public AtomicInteger getActivePRProcessingCount() {
        return activePRProcessingCount;
    }

    public AtomicInteger getQueuedPRCount() {
        return queuedPRCount;
    }

    public AtomicLong getTotalPRsProcessed() {
        return totalPRsProcessed;
    }

    public AtomicLong getFailedPRsCount() {
        return failedPRsCount;
    }

    public AtomicInteger getGithubApiCalls() {
        return githubApiCalls;
    }

    public AtomicInteger getGithubApiErrors() {
        return githubApiErrors;
    }

    public AtomicInteger getDbConnectionsActive() {
        return dbConnectionsActive;
    }

    public AtomicInteger getRedisConnectionsActive() {
        return redisConnectionsActive;
    }
}