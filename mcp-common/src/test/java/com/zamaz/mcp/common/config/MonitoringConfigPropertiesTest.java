package com.zamaz.mcp.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MonitoringConfigProperties.
 */
class MonitoringConfigPropertiesTest {

    private LocalValidatorFactoryBean validator;
    private MonitoringConfigProperties properties;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        properties = new MonitoringConfigProperties();
    }

    @Test
    void testValidConfiguration() {
        // Given
        properties.setEnabled(true);
        properties.setMetricsPath("/metrics");
        properties.setHealthPath("/health");
        properties.setExposedEndpoints(Arrays.asList("health", "metrics"));

        // When
        Errors errors = new BeanPropertyBindingResult(properties, "properties");
        validator.validate(properties, errors);

        // Then
        assertFalse(errors.hasErrors());
    }

    @Test
    void testMetricsConfiguration() {
        // Given
        MonitoringConfigProperties.Metrics metrics = properties.getMetrics();
        metrics.setPrometheusEnabled(true);
        metrics.setExportInterval(30);
        metrics.getTags().put("environment", "test");
        metrics.getTags().put("service", "test-service");
        metrics.setPercentiles(new double[]{0.5, 0.95, 0.99});
        metrics.setSloBuckets(new long[]{50, 100, 200, 500});

        // When
        Errors errors = new BeanPropertyBindingResult(metrics, "metrics");
        validator.validate(metrics, errors);

        // Then
        assertFalse(errors.hasErrors());
        assertTrue(metrics.isPrometheusEnabled());
        assertEquals(30, metrics.getExportInterval());
        assertEquals(2, metrics.getTags().size());
        assertEquals(3, metrics.getPercentiles().length);
        assertEquals(4, metrics.getSloBuckets().length);
    }

    @Test
    void testInvalidMetricsConfiguration_ExportIntervalTooLow() {
        // Given
        MonitoringConfigProperties.Metrics metrics = properties.getMetrics();
        metrics.setExportInterval(5); // Too low

        // When
        Errors errors = new BeanPropertyBindingResult(metrics, "metrics");
        validator.validate(metrics, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("exportInterval"));
    }

    @Test
    void testHealthConfiguration() {
        // Given
        MonitoringConfigProperties.Health health = properties.getHealth();
        health.setShowDetails("always");
        health.setShowComponents("when-authorized");
        health.setTimeout(10000);
        health.setIndicators(Arrays.asList("db", "redis", "custom"));

        MonitoringConfigProperties.Health.HealthGroup readinessGroup = new MonitoringConfigProperties.Health.HealthGroup();
        readinessGroup.setInclude(Arrays.asList("db", "redis"));
        readinessGroup.setShowDetails("always");
        health.getGroups().put("readiness", readinessGroup);

        // When
        Errors errors = new BeanPropertyBindingResult(health, "health");
        validator.validate(health, errors);

        // Then
        assertFalse(errors.hasErrors());
        assertEquals("always", health.getShowDetails());
        assertEquals("when-authorized", health.getShowComponents());
        assertEquals(10000, health.getTimeout());
        assertEquals(3, health.getIndicators().size());
        assertEquals(1, health.getGroups().size());
    }

    @Test
    void testInvalidHealthConfiguration_TimeoutTooLow() {
        // Given
        MonitoringConfigProperties.Health health = properties.getHealth();
        health.setTimeout(500); // Too low

        // When
        Errors errors = new BeanPropertyBindingResult(health, "health");
        validator.validate(health, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("timeout"));
    }

    @Test
    void testTracingConfiguration() {
        // Given
        MonitoringConfigProperties.Tracing tracing = properties.getTracing();
        tracing.setEnabled(true);
        tracing.setSamplingRate(0.5);
        tracing.setPropagationType("B3");
        tracing.setBaggageFields(Arrays.asList("userId", "requestId"));
        tracing.setEndpoint("http://jaeger:4317");
        tracing.setExportTimeout(30000);

        // When
        Errors errors = new BeanPropertyBindingResult(tracing, "tracing");
        validator.validate(tracing, errors);

        // Then
        assertFalse(errors.hasErrors());
        assertTrue(tracing.isEnabled());
        assertEquals(0.5, tracing.getSamplingRate());
        assertEquals("B3", tracing.getPropagationType());
        assertEquals(2, tracing.getBaggageFields().size());
    }

    @Test
    void testInvalidTracingConfiguration_InvalidSamplingRate() {
        // Given
        MonitoringConfigProperties.Tracing tracing = properties.getTracing();
        tracing.setSamplingRate(1.5); // Too high

        // When
        Errors errors = new BeanPropertyBindingResult(tracing, "tracing");
        validator.validate(tracing, errors);

        // Then
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getFieldErrorCount("samplingRate"));
    }

    @Test
    void testLoggingConfiguration() {
        // Given
        MonitoringConfigProperties.Logging logging = properties.getLogging();
        logging.setStructuredEnabled(true);
        logging.setFormat("json");
        logging.setIncludeMdc(true);
        logging.setIncludeStackTrace(false);
        logging.setAggregationEndpoint("http://logstash:5044");

        // Then
        assertTrue(logging.isStructuredEnabled());
        assertEquals("json", logging.getFormat());
        assertTrue(logging.isIncludeMdc());
        assertFalse(logging.isIncludeStackTrace());
        assertEquals("http://logstash:5044", logging.getAggregationEndpoint());
    }

    @Test
    void testAlertingConfiguration() {
        // Given
        MonitoringConfigProperties.Alerting alerting = properties.getAlerting();
        alerting.setEnabled(true);
        alerting.setChannels(Arrays.asList("email", "slack", "pagerduty"));
        alerting.setCooldownPeriod(600);

        MonitoringConfigProperties.Alerting.Thresholds thresholds = alerting.getThresholds();
        thresholds.setErrorRate(0.01);
        thresholds.setResponseTime(500);
        thresholds.setCpuUsage(0.90);
        thresholds.setMemoryUsage(0.95);
        thresholds.setDiskUsage(0.85);

        // Then
        assertTrue(alerting.isEnabled());
        assertEquals(3, alerting.getChannels().size());
        assertEquals(600, alerting.getCooldownPeriod());
        assertEquals(0.01, thresholds.getErrorRate());
        assertEquals(500, thresholds.getResponseTime());
        assertEquals(0.90, thresholds.getCpuUsage());
        assertEquals(0.95, thresholds.getMemoryUsage());
        assertEquals(0.85, thresholds.getDiskUsage());
    }

    @Test
    void testDefaultValues() {
        // Main defaults
        assertTrue(properties.isEnabled());
        assertEquals("/actuator/metrics", properties.getMetricsPath());
        assertEquals("/actuator/health", properties.getHealthPath());
        assertEquals("/actuator/info", properties.getInfoPath());
        assertEquals(Arrays.asList("health", "metrics", "info", "prometheus"), properties.getExposedEndpoints());

        // Metrics defaults
        MonitoringConfigProperties.Metrics metrics = properties.getMetrics();
        assertTrue(metrics.isPrometheusEnabled());
        assertEquals(60, metrics.getExportInterval());
        assertEquals(0.5, metrics.getPercentiles()[0]);
        assertEquals(100, metrics.getSloBuckets()[0]);

        // Health defaults
        MonitoringConfigProperties.Health health = properties.getHealth();
        assertEquals("when-authorized", health.getShowDetails());
        assertEquals(5000, health.getTimeout());

        // Tracing defaults
        MonitoringConfigProperties.Tracing tracing = properties.getTracing();
        assertTrue(tracing.isEnabled());
        assertEquals(0.1, tracing.getSamplingRate());
        assertEquals("B3,W3C", tracing.getPropagationType());

        // Alerting defaults
        MonitoringConfigProperties.Alerting alerting = properties.getAlerting();
        assertFalse(alerting.isEnabled());
        assertEquals(0.05, alerting.getThresholds().getErrorRate());
        assertEquals(1000, alerting.getThresholds().getResponseTime());
    }

    @Test
    void testHistogramConfiguration() {
        // Given
        MonitoringConfigProperties.Metrics.Histogram histogram = properties.getMetrics().getHistogram();
        histogram.setEnabled(true);
        histogram.setBuckets(20);
        histogram.setPercentileHistogram(false);

        // Then
        assertTrue(histogram.isEnabled());
        assertEquals(20, histogram.getBuckets());
        assertFalse(histogram.isPercentileHistogram());
    }
}