package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.config.MetricsConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Service for managing and updating custom metrics.
 * This service provides a centralized way to record metrics from various components
 * of the GitHub integration system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final MetricsConfig metricsConfig;

    /**
     * Records PR processing metrics
     */
    public void recordPRProcessingStart(String repository, String prNumber) {
        metricsConfig.getActivePRProcessingCount().incrementAndGet();
        metricsConfig.getQueuedPRCount().decrementAndGet();
        
        meterRegistry.counter("github_integration_pr_processing_started_total",
                "repository", repository,
                "pr_number", prNumber).increment();
        
        log.debug("Started PR processing for repo: {}, PR: {}", repository, prNumber);
    }

    public void recordPRProcessingComplete(String repository, String prNumber, Duration duration, boolean success) {
        metricsConfig.getActivePRProcessingCount().decrementAndGet();
        metricsConfig.getTotalPRsProcessed().incrementAndGet();
        
        if (!success) {
            metricsConfig.getFailedPRsCount().incrementAndGet();
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("github_integration_pr_processing_duration")
                .tag("repository", repository)
                .tag("pr_number", prNumber)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));

        meterRegistry.counter("github_integration_pr_processing_completed_total",
                "repository", repository,
                "pr_number", prNumber,
                "success", String.valueOf(success)).increment();
        
        log.debug("Completed PR processing for repo: {}, PR: {}, success: {}, duration: {}ms", 
                repository, prNumber, success, duration.toMillis());
    }

    /**
     * Records GitHub API metrics
     */
    public void recordGitHubApiCall(String endpoint, String method) {
        metricsConfig.getGithubApiCalls().incrementAndGet();
        
        meterRegistry.counter("github_integration_github_api_calls_total",
                "endpoint", endpoint,
                "method", method).increment();
    }

    public void recordGitHubApiError(String endpoint, String method, String errorType) {
        metricsConfig.getGithubApiErrors().incrementAndGet();
        
        meterRegistry.counter("github_integration_github_api_errors_total",
                "endpoint", endpoint,
                "method", method,
                "error_type", errorType).increment();
        
        log.warn("GitHub API error for endpoint: {}, method: {}, error: {}", endpoint, method, errorType);
    }

    public void recordGitHubApiResponse(String endpoint, String method, Duration duration, int statusCode) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("github_integration_github_api_response_time")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status_code", String.valueOf(statusCode))
                .register(meterRegistry));
        
        log.debug("GitHub API response for endpoint: {}, method: {}, status: {}, duration: {}ms", 
                endpoint, method, statusCode, duration.toMillis());
    }

    public void recordGitHubApiRateLimit(int remaining, int limit) {
        meterRegistry.gauge("github_integration_github_api_rate_limit_remaining", remaining);
        meterRegistry.gauge("github_integration_github_api_rate_limit_total", limit);
        
        if (remaining < limit * 0.1) { // Less than 10% remaining
            log.warn("GitHub API rate limit low: {} remaining out of {}", remaining, limit);
        }
    }

    public void recordGitHubApiRetry(String endpoint, String method, int attempt) {
        meterRegistry.counter("github_integration_github_api_retries_total",
                "endpoint", endpoint,
                "method", method,
                "attempt", String.valueOf(attempt)).increment();
        
        log.debug("GitHub API retry for endpoint: {}, method: {}, attempt: {}", endpoint, method, attempt);
    }

    /**
     * Records database metrics
     */
    public void recordDatabaseQuery(String operation, String table, Duration duration) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("github_integration_database_query_duration")
                .tag("operation", operation)
                .tag("table", table)
                .register(meterRegistry));
        
        log.debug("Database query completed - operation: {}, table: {}, duration: {}ms", 
                operation, table, duration.toMillis());
    }

    public void recordDatabaseConnectionChange(int activeConnections) {
        metricsConfig.getDbConnectionsActive().set(activeConnections);
        meterRegistry.gauge("github_integration_database_connections_active", activeConnections);
    }

    /**
     * Records Redis metrics
     */
    public void recordRedisOperation(String operation, boolean success, Duration duration) {
        meterRegistry.counter("github_integration_redis_operations_total",
                "operation", operation,
                "success", String.valueOf(success)).increment();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("github_integration_redis_operation_duration")
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        
        log.debug("Redis operation completed - operation: {}, success: {}, duration: {}ms", 
                operation, success, duration.toMillis());
    }

    public void recordCacheHitRatio(double hitRatio) {
        meterRegistry.gauge("github_integration_cache_hit_ratio", hitRatio);
    }

    public void recordRedisConnectionChange(int activeConnections) {
        metricsConfig.getRedisConnectionsActive().set(activeConnections);
        meterRegistry.gauge("github_integration_redis_connections_active", activeConnections);
    }

    /**
     * Records business logic metrics
     */
    public void recordReviewCommentGenerated(String repository, String prNumber, String issueType) {
        meterRegistry.counter("github_integration_review_comments_generated_total",
                "repository", repository,
                "pr_number", prNumber,
                "issue_type", issueType).increment();
    }

    public void recordIssueDetected(String repository, String prNumber, String issueType, String severity) {
        meterRegistry.counter("github_integration_issues_detected_total",
                "repository", repository,
                "pr_number", prNumber,
                "issue_type", issueType,
                "severity", severity).increment();
    }

    public void recordAutoFixApplied(String repository, String prNumber, String fixType) {
        meterRegistry.counter("github_integration_auto_fixes_applied_total",
                "repository", repository,
                "pr_number", prNumber,
                "fix_type", fixType).increment();
    }

    public void recordWebhookEvent(String eventType, String repository, boolean processed) {
        meterRegistry.counter("github_integration_webhook_events_processed_total",
                "event_type", eventType,
                "repository", repository,
                "processed", String.valueOf(processed)).increment();
    }

    public void recordRepositoryCount(int count) {
        meterRegistry.gauge("github_integration_configured_repositories", count);
    }

    /**
     * Records file analysis metrics
     */
    public void recordFileAnalysis(String repository, String fileName, String fileType, 
                                 Duration duration, int issuesFound) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("github_integration_file_analysis_duration")
                .tag("repository", repository)
                .tag("file_type", fileType)
                .register(meterRegistry));

        meterRegistry.counter("github_integration_files_analyzed_total",
                "repository", repository,
                "file_type", fileType).increment();

        meterRegistry.counter("github_integration_issues_found_in_files_total",
                "repository", repository,
                "file_type", fileType).increment(issuesFound);
        
        log.debug("File analysis completed - repository: {}, file: {}, type: {}, duration: {}ms, issues: {}", 
                repository, fileName, fileType, duration.toMillis(), issuesFound);
    }

    /**
     * Records review generation metrics
     */
    public void recordReviewGeneration(String repository, String prNumber, Duration duration, 
                                     int commentsGenerated, boolean success) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("github_integration_review_generation_duration")
                .tag("repository", repository)
                .tag("pr_number", prNumber)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));

        meterRegistry.counter("github_integration_reviews_generated_total",
                "repository", repository,
                "pr_number", prNumber,
                "success", String.valueOf(success)).increment();

        meterRegistry.gauge("github_integration_review_comments_count", commentsGenerated);
        
        log.debug("Review generation completed - repository: {}, PR: {}, success: {}, duration: {}ms, comments: {}", 
                repository, prNumber, success, duration.toMillis(), commentsGenerated);
    }

    /**
     * Utility method to time operations
     */
    public <T> T timeOperation(String operationName, Callable<T> operation) throws Exception {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = operation.call();
            sample.stop(Timer.builder("github_integration_operation_duration")
                    .tag("operation", operationName)
                    .tag("success", "true")
                    .register(meterRegistry));
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("github_integration_operation_duration")
                    .tag("operation", operationName)
                    .tag("success", "false")
                    .register(meterRegistry));
            throw e;
        }
    }

    /**
     * Records custom SLO metrics
     */
    public void recordSLOMetric(String sloType, double value) {
        meterRegistry.gauge("github_integration_slo_" + sloType, value);
    }

    /**
     * Increments PR queue count
     */
    public void incrementPRQueue() {
        metricsConfig.getQueuedPRCount().incrementAndGet();
    }

    /**
     * Decrements PR queue count
     */
    public void decrementPRQueue() {
        metricsConfig.getQueuedPRCount().decrementAndGet();
    }
}