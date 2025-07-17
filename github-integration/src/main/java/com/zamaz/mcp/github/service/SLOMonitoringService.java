package com.zamaz.mcp.github.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring Service Level Objectives (SLOs).
 * This service tracks and reports on key SLO metrics for the GitHub integration service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SLOMonitoringService {

    private final MeterRegistry meterRegistry;
    private final MetricsService metricsService;

    // SLO Thresholds (configurable via application.yml)
    @Value("${app.slo.pr-processing-p95-threshold:5000}")
    private long prProcessingP95ThresholdMs;

    @Value("${app.slo.error-rate-threshold:0.05}")
    private double errorRateThreshold;

    @Value("${app.slo.availability-threshold:0.995}")
    private double availabilityThreshold;

    @Value("${app.slo.github-api-p95-threshold:2000}")
    private long githubApiP95ThresholdMs;

    // Rolling window counters for SLO calculation
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);
    private final AtomicLong uptimeSeconds = new AtomicLong(0);
    private final AtomicLong downtimeSeconds = new AtomicLong(0);

    private LocalDateTime lastSLOCalculation = LocalDateTime.now();

    /**
     * Calculates and reports SLO metrics every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void calculateSLOMetrics() {
        try {
            calculateAndReportPRProcessingSLO();
            calculateAndReportErrorRateSLO();
            calculateAndReportAvailabilitySLO();
            calculateAndReportGitHubApiSLO();
            calculateAndReportOverallSLO();
            
            lastSLOCalculation = LocalDateTime.now();
            log.debug("SLO metrics calculated and reported successfully");
        } catch (Exception e) {
            log.error("Error calculating SLO metrics", e);
        }
    }

    /**
     * Calculates PR processing P95 latency SLO
     */
    private void calculateAndReportPRProcessingSLO() {
        Timer prProcessingTimer = meterRegistry.find("github_integration_pr_processing_duration").timer();
        
        if (prProcessingTimer != null) {
            double p95Value = prProcessingTimer.percentile(0.95);
            double p95ValueMs = p95Value * 1000; // Convert to milliseconds
            
            boolean p95Compliant = p95ValueMs <= prProcessingP95ThresholdMs;
            double complianceRatio = p95Compliant ? 1.0 : 0.0;
            
            metricsService.recordSLOMetric("pr_processing_p95_compliance", complianceRatio);
            metricsService.recordSLOMetric("pr_processing_p95_latency_ms", p95ValueMs);
            
            // Alert if SLO is violated
            if (!p95Compliant) {
                log.warn("PR Processing P95 SLO violated: {}ms > {}ms threshold", 
                        p95ValueMs, prProcessingP95ThresholdMs);
                
                meterRegistry.counter("github_integration_slo_violations_total",
                        "slo_type", "pr_processing_p95").increment();
            }
            
            log.debug("PR Processing P95 SLO: {}ms (compliant: {})", p95ValueMs, p95Compliant);
        }
    }

    /**
     * Calculates error rate SLO
     */
    private void calculateAndReportErrorRateSLO() {
        // Calculate error rate from HTTP requests
        double errorRate = calculateErrorRate();
        
        boolean errorRateCompliant = errorRate <= errorRateThreshold;
        double complianceRatio = errorRateCompliant ? 1.0 : 0.0;
        
        metricsService.recordSLOMetric("error_rate_compliance", complianceRatio);
        metricsService.recordSLOMetric("error_rate", errorRate);
        
        // Alert if SLO is violated
        if (!errorRateCompliant) {
            log.warn("Error Rate SLO violated: {} > {} threshold", errorRate, errorRateThreshold);
            
            meterRegistry.counter("github_integration_slo_violations_total",
                    "slo_type", "error_rate").increment();
        }
        
        log.debug("Error Rate SLO: {} (compliant: {})", errorRate, errorRateCompliant);
    }

    /**
     * Calculates availability SLO
     */
    private void calculateAndReportAvailabilitySLO() {
        // Calculate uptime based on health checks
        double availability = calculateAvailability();
        
        boolean availabilityCompliant = availability >= availabilityThreshold;
        double complianceRatio = availabilityCompliant ? 1.0 : 0.0;
        
        metricsService.recordSLOMetric("availability_compliance", complianceRatio);
        metricsService.recordSLOMetric("availability", availability);
        
        // Alert if SLO is violated
        if (!availabilityCompliant) {
            log.warn("Availability SLO violated: {} < {} threshold", availability, availabilityThreshold);
            
            meterRegistry.counter("github_integration_slo_violations_total",
                    "slo_type", "availability").increment();
        }
        
        log.debug("Availability SLO: {} (compliant: {})", availability, availabilityCompliant);
    }

    /**
     * Calculates GitHub API response time SLO
     */
    private void calculateAndReportGitHubApiSLO() {
        Timer githubApiTimer = meterRegistry.find("github_integration_github_api_response_time").timer();
        
        if (githubApiTimer != null) {
            double p95Value = githubApiTimer.percentile(0.95);
            double p95ValueMs = p95Value * 1000; // Convert to milliseconds
            
            boolean p95Compliant = p95ValueMs <= githubApiP95ThresholdMs;
            double complianceRatio = p95Compliant ? 1.0 : 0.0;
            
            metricsService.recordSLOMetric("github_api_p95_compliance", complianceRatio);
            metricsService.recordSLOMetric("github_api_p95_latency_ms", p95ValueMs);
            
            // Alert if SLO is violated
            if (!p95Compliant) {
                log.warn("GitHub API P95 SLO violated: {}ms > {}ms threshold", 
                        p95ValueMs, githubApiP95ThresholdMs);
                
                meterRegistry.counter("github_integration_slo_violations_total",
                        "slo_type", "github_api_p95").increment();
            }
            
            log.debug("GitHub API P95 SLO: {}ms (compliant: {})", p95ValueMs, p95Compliant);
        }
    }

    /**
     * Calculates overall SLO compliance
     */
    private void calculateAndReportOverallSLO() {
        // Get individual SLO compliance metrics
        double prProcessingCompliance = getSLOMetric("pr_processing_p95_compliance");
        double errorRateCompliance = getSLOMetric("error_rate_compliance");
        double availabilityCompliance = getSLOMetric("availability_compliance");
        double githubApiCompliance = getSLOMetric("github_api_p95_compliance");
        
        // Calculate weighted overall compliance
        double overallCompliance = (prProcessingCompliance * 0.3 + 
                                   errorRateCompliance * 0.3 +
                                   availabilityCompliance * 0.3 +
                                   githubApiCompliance * 0.1);
        
        metricsService.recordSLOMetric("overall_compliance", overallCompliance);
        
        // Generate SLO report
        if (overallCompliance < 0.99) {
            log.warn("Overall SLO compliance below 99%: {}", overallCompliance);
            
            meterRegistry.counter("github_integration_slo_violations_total",
                    "slo_type", "overall").increment();
        }
        
        log.debug("Overall SLO compliance: {}", overallCompliance);
    }

    /**
     * Helper method to calculate error rate from HTTP metrics
     */
    private double calculateErrorRate() {
        // Get HTTP error count (5xx responses)
        var errorCounter = meterRegistry.find("http_server_requests_seconds_count")
                .tags("status", "5xx");
        
        // Get total HTTP request count
        var totalCounter = meterRegistry.find("http_server_requests_seconds_count");
        
        if (errorCounter != null && totalCounter != null) {
            double errorCount = errorCounter.counter().count();
            double totalCount = totalCounter.counter().count();
            
            if (totalCount > 0) {
                return errorCount / totalCount;
            }
        }
        
        return 0.0;
    }

    /**
     * Helper method to calculate availability based on uptime
     */
    private double calculateAvailability() {
        long totalTime = uptimeSeconds.get() + downtimeSeconds.get();
        
        if (totalTime > 0) {
            return (double) uptimeSeconds.get() / totalTime;
        }
        
        return 1.0; // Assume 100% availability if no data
    }

    /**
     * Helper method to get SLO metric value
     */
    private double getSLOMetric(String metricName) {
        var gauge = meterRegistry.find("github_integration_slo_" + metricName).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    /**
     * Records uptime/downtime for availability calculation
     */
    public void recordUptime(long seconds) {
        uptimeSeconds.addAndGet(seconds);
    }

    public void recordDowntime(long seconds) {
        downtimeSeconds.addAndGet(seconds);
    }

    /**
     * Records request counts for error rate calculation
     */
    public void recordRequest(boolean isError) {
        totalRequests.incrementAndGet();
        if (isError) {
            errorRequests.incrementAndGet();
        }
    }

    /**
     * Gets current SLO status summary
     */
    public SLOStatus getCurrentSLOStatus() {
        return SLOStatus.builder()
                .prProcessingP95Compliance(getSLOMetric("pr_processing_p95_compliance"))
                .errorRateCompliance(getSLOMetric("error_rate_compliance"))
                .availabilityCompliance(getSLOMetric("availability_compliance"))
                .githubApiP95Compliance(getSLOMetric("github_api_p95_compliance"))
                .overallCompliance(getSLOMetric("overall_compliance"))
                .lastCalculated(lastSLOCalculation)
                .build();
    }

    /**
     * SLO Status data class
     */
    public static class SLOStatus {
        private double prProcessingP95Compliance;
        private double errorRateCompliance;
        private double availabilityCompliance;
        private double githubApiP95Compliance;
        private double overallCompliance;
        private LocalDateTime lastCalculated;

        public static SLOStatusBuilder builder() {
            return new SLOStatusBuilder();
        }

        public static class SLOStatusBuilder {
            private double prProcessingP95Compliance;
            private double errorRateCompliance;
            private double availabilityCompliance;
            private double githubApiP95Compliance;
            private double overallCompliance;
            private LocalDateTime lastCalculated;

            public SLOStatusBuilder prProcessingP95Compliance(double prProcessingP95Compliance) {
                this.prProcessingP95Compliance = prProcessingP95Compliance;
                return this;
            }

            public SLOStatusBuilder errorRateCompliance(double errorRateCompliance) {
                this.errorRateCompliance = errorRateCompliance;
                return this;
            }

            public SLOStatusBuilder availabilityCompliance(double availabilityCompliance) {
                this.availabilityCompliance = availabilityCompliance;
                return this;
            }

            public SLOStatusBuilder githubApiP95Compliance(double githubApiP95Compliance) {
                this.githubApiP95Compliance = githubApiP95Compliance;
                return this;
            }

            public SLOStatusBuilder overallCompliance(double overallCompliance) {
                this.overallCompliance = overallCompliance;
                return this;
            }

            public SLOStatusBuilder lastCalculated(LocalDateTime lastCalculated) {
                this.lastCalculated = lastCalculated;
                return this;
            }

            public SLOStatus build() {
                SLOStatus status = new SLOStatus();
                status.prProcessingP95Compliance = this.prProcessingP95Compliance;
                status.errorRateCompliance = this.errorRateCompliance;
                status.availabilityCompliance = this.availabilityCompliance;
                status.githubApiP95Compliance = this.githubApiP95Compliance;
                status.overallCompliance = this.overallCompliance;
                status.lastCalculated = this.lastCalculated;
                return status;
            }
        }

        // Getters
        public double getPrProcessingP95Compliance() { return prProcessingP95Compliance; }
        public double getErrorRateCompliance() { return errorRateCompliance; }
        public double getAvailabilityCompliance() { return availabilityCompliance; }
        public double getGithubApiP95Compliance() { return githubApiP95Compliance; }
        public double getOverallCompliance() { return overallCompliance; }
        public LocalDateTime getLastCalculated() { return lastCalculated; }
    }
}