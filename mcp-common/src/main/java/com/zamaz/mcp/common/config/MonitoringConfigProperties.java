package com.zamaz.mcp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Common monitoring configuration properties for actuator and metrics.
 * These properties are loaded from the centralized configuration server.
 */
@ConfigurationProperties(prefix = "mcp.monitoring")
@Validated
public class MonitoringConfigProperties {

    /**
     * Whether monitoring is enabled
     */
    private boolean enabled = true;

    /**
     * Metrics path
     */
    private String metricsPath = "/actuator/metrics";

    /**
     * Health check path
     */
    private String healthPath = "/actuator/health";

    /**
     * Info endpoint path
     */
    private String infoPath = "/actuator/info";

    /**
     * Exposed actuator endpoints
     */
    private List<String> exposedEndpoints = Arrays.asList("health", "metrics", "info", "prometheus");

    /**
     * Metrics configuration
     */
    private Metrics metrics = new Metrics();

    /**
     * Health check configuration
     */
    private Health health = new Health();

    /**
     * Tracing configuration
     */
    private Tracing tracing = new Tracing();

    /**
     * Logging configuration
     */
    private Logging logging = new Logging();

    /**
     * Alerting configuration
     */
    private Alerting alerting = new Alerting();

    /**
     * Metrics configuration
     */
    @Validated
    public static class Metrics {
        /**
         * Whether to export metrics to Prometheus
         */
        private boolean prometheusEnabled = true;

        /**
         * Metrics export interval in seconds
         */
        @Min(value = 10, message = "Export interval must be at least 10 seconds")
        @Max(value = 300, message = "Export interval must not exceed 300 seconds")
        private int exportInterval = 60;

        /**
         * Common tags to add to all metrics
         */
        private Map<String, String> tags = new HashMap<>();

        /**
         * Histogram configuration
         */
        private Histogram histogram = new Histogram();

        /**
         * Percentiles to calculate
         */
        private double[] percentiles = {0.5, 0.75, 0.95, 0.99};

        /**
         * SLO buckets for response time in milliseconds
         */
        private long[] sloBuckets = {100, 200, 400, 800, 1600};

        /**
         * Histogram configuration
         */
        public static class Histogram {
            private boolean enabled = true;
            private int buckets = 10;
            private boolean percentileHistogram = true;

            // Getters and setters
            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getBuckets() {
                return buckets;
            }

            public void setBuckets(int buckets) {
                this.buckets = buckets;
            }

            public boolean isPercentileHistogram() {
                return percentileHistogram;
            }

            public void setPercentileHistogram(boolean percentileHistogram) {
                this.percentileHistogram = percentileHistogram;
            }
        }

        // Getters and setters
        public boolean isPrometheusEnabled() {
            return prometheusEnabled;
        }

        public void setPrometheusEnabled(boolean prometheusEnabled) {
            this.prometheusEnabled = prometheusEnabled;
        }

        public int getExportInterval() {
            return exportInterval;
        }

        public void setExportInterval(int exportInterval) {
            this.exportInterval = exportInterval;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }

        public Histogram getHistogram() {
            return histogram;
        }

        public void setHistogram(Histogram histogram) {
            this.histogram = histogram;
        }

        public double[] getPercentiles() {
            return percentiles;
        }

        public void setPercentiles(double[] percentiles) {
            this.percentiles = percentiles;
        }

        public long[] getSloBuckets() {
            return sloBuckets;
        }

        public void setSloBuckets(long[] sloBuckets) {
            this.sloBuckets = sloBuckets;
        }
    }

    /**
     * Health check configuration
     */
    @Validated
    public static class Health {
        /**
         * Whether to show health details
         */
        private String showDetails = "when-authorized";

        /**
         * Whether to show components
         */
        private String showComponents = "when-authorized";

        /**
         * Health check groups
         */
        private Map<String, HealthGroup> groups = new HashMap<>();

        /**
         * Custom health indicators
         */
        private List<String> indicators = Arrays.asList("db", "redis", "diskSpace", "mail");

        /**
         * Health check timeouts in milliseconds
         */
        @Min(value = 1000, message = "Timeout must be at least 1000ms")
        private long timeout = 5000;

        /**
         * Health group configuration
         */
        public static class HealthGroup {
            private List<String> include = new ArrayList<>();
            private List<String> exclude = new ArrayList<>();
            private String showDetails = "always";

            // Getters and setters
            public List<String> getInclude() {
                return include;
            }

            public void setInclude(List<String> include) {
                this.include = include;
            }

            public List<String> getExclude() {
                return exclude;
            }

            public void setExclude(List<String> exclude) {
                this.exclude = exclude;
            }

            public String getShowDetails() {
                return showDetails;
            }

            public void setShowDetails(String showDetails) {
                this.showDetails = showDetails;
            }
        }

        // Getters and setters
        public String getShowDetails() {
            return showDetails;
        }

        public void setShowDetails(String showDetails) {
            this.showDetails = showDetails;
        }

        public String getShowComponents() {
            return showComponents;
        }

        public void setShowComponents(String showComponents) {
            this.showComponents = showComponents;
        }

        public Map<String, HealthGroup> getGroups() {
            return groups;
        }

        public void setGroups(Map<String, HealthGroup> groups) {
            this.groups = groups;
        }

        public List<String> getIndicators() {
            return indicators;
        }

        public void setIndicators(List<String> indicators) {
            this.indicators = indicators;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Tracing configuration
     */
    @Validated
    public static class Tracing {
        /**
         * Whether tracing is enabled
         */
        private boolean enabled = true;

        /**
         * Sampling rate (0.0 to 1.0)
         */
        @Min(value = 0, message = "Sampling rate must be at least 0")
        @Max(value = 1, message = "Sampling rate must not exceed 1")
        private double samplingRate = 0.1;

        /**
         * Trace propagation type
         */
        private String propagationType = "B3,W3C";

        /**
         * Baggage fields to propagate
         */
        private List<String> baggageFields = Arrays.asList("userId", "organizationId", "requestId");

        /**
         * Endpoint for trace collection
         */
        private String endpoint = "http://localhost:4317";

        /**
         * Export timeout in milliseconds
         */
        private long exportTimeout = 10000;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(double samplingRate) {
            this.samplingRate = samplingRate;
        }

        public String getPropagationType() {
            return propagationType;
        }

        public void setPropagationType(String propagationType) {
            this.propagationType = propagationType;
        }

        public List<String> getBaggageFields() {
            return baggageFields;
        }

        public void setBaggageFields(List<String> baggageFields) {
            this.baggageFields = baggageFields;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public long getExportTimeout() {
            return exportTimeout;
        }

        public void setExportTimeout(long exportTimeout) {
            this.exportTimeout = exportTimeout;
        }
    }

    /**
     * Logging configuration
     */
    public static class Logging {
        /**
         * Whether structured logging is enabled
         */
        private boolean structuredEnabled = true;

        /**
         * Log format (json, logstash, plain)
         */
        private String format = "json";

        /**
         * Whether to include MDC fields
         */
        private boolean includeMdc = true;

        /**
         * Whether to include stack traces
         */
        private boolean includeStackTrace = true;

        /**
         * Log aggregation endpoint
         */
        private String aggregationEndpoint;

        // Getters and setters
        public boolean isStructuredEnabled() {
            return structuredEnabled;
        }

        public void setStructuredEnabled(boolean structuredEnabled) {
            this.structuredEnabled = structuredEnabled;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public boolean isIncludeMdc() {
            return includeMdc;
        }

        public void setIncludeMdc(boolean includeMdc) {
            this.includeMdc = includeMdc;
        }

        public boolean isIncludeStackTrace() {
            return includeStackTrace;
        }

        public void setIncludeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
        }

        public String getAggregationEndpoint() {
            return aggregationEndpoint;
        }

        public void setAggregationEndpoint(String aggregationEndpoint) {
            this.aggregationEndpoint = aggregationEndpoint;
        }
    }

    /**
     * Alerting configuration
     */
    public static class Alerting {
        /**
         * Whether alerting is enabled
         */
        private boolean enabled = false;

        /**
         * Alert thresholds
         */
        private Thresholds thresholds = new Thresholds();

        /**
         * Alert channels
         */
        private List<String> channels = Arrays.asList("email", "slack");

        /**
         * Alert cooldown period in seconds
         */
        private int cooldownPeriod = 300;

        /**
         * Alert thresholds
         */
        public static class Thresholds {
            private double errorRate = 0.05;
            private long responseTime = 1000;
            private double cpuUsage = 0.80;
            private double memoryUsage = 0.85;
            private double diskUsage = 0.90;

            // Getters and setters
            public double getErrorRate() {
                return errorRate;
            }

            public void setErrorRate(double errorRate) {
                this.errorRate = errorRate;
            }

            public long getResponseTime() {
                return responseTime;
            }

            public void setResponseTime(long responseTime) {
                this.responseTime = responseTime;
            }

            public double getCpuUsage() {
                return cpuUsage;
            }

            public void setCpuUsage(double cpuUsage) {
                this.cpuUsage = cpuUsage;
            }

            public double getMemoryUsage() {
                return memoryUsage;
            }

            public void setMemoryUsage(double memoryUsage) {
                this.memoryUsage = memoryUsage;
            }

            public double getDiskUsage() {
                return diskUsage;
            }

            public void setDiskUsage(double diskUsage) {
                this.diskUsage = diskUsage;
            }
        }

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Thresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(Thresholds thresholds) {
            this.thresholds = thresholds;
        }

        public List<String> getChannels() {
            return channels;
        }

        public void setChannels(List<String> channels) {
            this.channels = channels;
        }

        public int getCooldownPeriod() {
            return cooldownPeriod;
        }

        public void setCooldownPeriod(int cooldownPeriod) {
            this.cooldownPeriod = cooldownPeriod;
        }
    }

    // Getters and setters for main properties
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMetricsPath() {
        return metricsPath;
    }

    public void setMetricsPath(String metricsPath) {
        this.metricsPath = metricsPath;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public String getInfoPath() {
        return infoPath;
    }

    public void setInfoPath(String infoPath) {
        this.infoPath = infoPath;
    }

    public List<String> getExposedEndpoints() {
        return exposedEndpoints;
    }

    public void setExposedEndpoints(List<String> exposedEndpoints) {
        this.exposedEndpoints = exposedEndpoints;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Health getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health;
    }

    public Tracing getTracing() {
        return tracing;
    }

    public void setTracing(Tracing tracing) {
        this.tracing = tracing;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Alerting getAlerting() {
        return alerting;
    }

    public void setAlerting(Alerting alerting) {
        this.alerting = alerting;
    }
}