package com.zamaz.mcp.common.testing.chaos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Chaos Engineering Framework for testing system resilience.
 * Provides controlled failure injection and system behavior validation.
 */
public class ChaosEngineeringFramework {

    private final List<ChaosExperiment> experiments = new ArrayList<>();
    private final ChaosConfiguration configuration;
    private final ExecutorService executor;
    private final SystemHealthMonitor healthMonitor;

    public ChaosEngineeringFramework(ChaosConfiguration configuration) {
        this.configuration = configuration;
        this.executor = Executors.newFixedThreadPool(configuration.getConcurrency());
        this.healthMonitor = new SystemHealthMonitor();
    }

    /**
     * Builder for creating chaos experiments.
     */
    public static class ChaosExperimentBuilder {
        private String name;
        private String description;
        private Duration duration = Duration.ofMinutes(5);
        private final List<FailureInjection> failureInjections = new ArrayList<>();
        private final List<SystemAssertion> assertions = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private boolean enabled = true;

        public ChaosExperimentBuilder named(String name) {
            this.name = name;
            return this;
        }

        public ChaosExperimentBuilder describedAs(String description) {
            this.description = description;
            return this;
        }

        public ChaosExperimentBuilder runningFor(Duration duration) {
            this.duration = duration;
            return this;
        }

        public ChaosExperimentBuilder withFailure(FailureInjection injection) {
            this.failureInjections.add(injection);
            return this;
        }

        public ChaosExperimentBuilder withAssertion(SystemAssertion assertion) {
            this.assertions.add(assertion);
            return this;
        }

        public ChaosExperimentBuilder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public ChaosExperimentBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ChaosExperiment build() {
            return new ChaosExperiment(name, description, duration, 
                                     failureInjections, assertions, metadata, enabled);
        }
    }

    /**
     * Represents a chaos engineering experiment.
     */
    public static class ChaosExperiment {
        private final String name;
        private final String description;
        private final Duration duration;
        private final List<FailureInjection> failureInjections;
        private final List<SystemAssertion> assertions;
        private final Map<String, Object> metadata;
        private final boolean enabled;
        private ExperimentResult result;

        public ChaosExperiment(String name, String description, Duration duration,
                              List<FailureInjection> failureInjections,
                              List<SystemAssertion> assertions,
                              Map<String, Object> metadata, boolean enabled) {
            this.name = name;
            this.description = description;
            this.duration = duration;
            this.failureInjections = new ArrayList<>(failureInjections);
            this.assertions = new ArrayList<>(assertions);
            this.metadata = new HashMap<>(metadata);
            this.enabled = enabled;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Duration getDuration() { return duration; }
        public List<FailureInjection> getFailureInjections() { return failureInjections; }
        public List<SystemAssertion> getAssertions() { return assertions; }
        public Map<String, Object> getMetadata() { return metadata; }
        public boolean isEnabled() { return enabled; }
        public ExperimentResult getResult() { return result; }
        public void setResult(ExperimentResult result) { this.result = result; }
    }

    /**
     * Failure injection interface.
     */
    public interface FailureInjection {
        String getName();
        void inject() throws Exception;
        void recover() throws Exception;
        boolean isRecoverable();
    }

    /**
     * System assertion interface.
     */
    public interface SystemAssertion {
        String getName();
        boolean verify() throws Exception;
        String getFailureMessage();
    }

    /**
     * Network latency injection.
     */
    public static class NetworkLatencyInjection implements FailureInjection {
        private final String serviceName;
        private final Duration latency;
        private final double probability;
        private volatile boolean active = false;

        public NetworkLatencyInjection(String serviceName, Duration latency, double probability) {
            this.serviceName = serviceName;
            this.latency = latency;
            this.probability = probability;
        }

        @Override
        public String getName() {
            return "Network Latency: " + serviceName + " +" + latency.toMillis() + "ms";
        }

        @Override
        public void inject() throws Exception {
            active = true;
            // In a real implementation, this would configure network proxy/rules
            // For testing, we might use WireMock or similar tools
        }

        @Override
        public void recover() throws Exception {
            active = false;
            // Remove network latency rules
        }

        @Override
        public boolean isRecoverable() {
            return true;
        }

        public boolean isActive() { return active; }
        public Duration getLatency() { return latency; }
        public double getProbability() { return probability; }
    }

    /**
     * Service unavailability injection.
     */
    public static class ServiceUnavailabilityInjection implements FailureInjection {
        private final String serviceName;
        private final Duration downtime;
        private volatile boolean serviceDown = false;

        public ServiceUnavailabilityInjection(String serviceName, Duration downtime) {
            this.serviceName = serviceName;
            this.downtime = downtime;
        }

        @Override
        public String getName() {
            return "Service Unavailable: " + serviceName + " for " + downtime.toSeconds() + "s";
        }

        @Override
        public void inject() throws Exception {
            serviceDown = true;
            // In real implementation, this would stop the service or block traffic
        }

        @Override
        public void recover() throws Exception {
            serviceDown = false;
            // Restore service availability
        }

        @Override
        public boolean isRecoverable() {
            return true;
        }

        public boolean isServiceDown() { return serviceDown; }
    }

    /**
     * Database connection failure injection.
     */
    public static class DatabaseFailureInjection implements FailureInjection {
        private final String databaseName;
        private final FailureType failureType;
        private volatile boolean active = false;

        public enum FailureType {
            CONNECTION_TIMEOUT,
            SLOW_QUERIES,
            CONNECTION_POOL_EXHAUSTION,
            DEADLOCKS
        }

        public DatabaseFailureInjection(String databaseName, FailureType failureType) {
            this.databaseName = databaseName;
            this.failureType = failureType;
        }

        @Override
        public String getName() {
            return "Database Failure: " + databaseName + " - " + failureType;
        }

        @Override
        public void inject() throws Exception {
            active = true;
            // Implement database failure simulation
            switch (failureType) {
                case CONNECTION_TIMEOUT:
                    // Configure connection timeouts
                    break;
                case SLOW_QUERIES:
                    // Inject query delays
                    break;
                case CONNECTION_POOL_EXHAUSTION:
                    // Exhaust connection pool
                    break;
                case DEADLOCKS:
                    // Create deadlock scenarios
                    break;
            }
        }

        @Override
        public void recover() throws Exception {
            active = false;
            // Restore database functionality
        }

        @Override
        public boolean isRecoverable() {
            return true;
        }
    }

    /**
     * Memory pressure injection.
     */
    public static class MemoryPressureInjection implements FailureInjection {
        private final long memoryToConsume;
        private final List<byte[]> memoryHogs = new ArrayList<>();
        private volatile boolean active = false;

        public MemoryPressureInjection(long memoryToConsume) {
            this.memoryToConsume = memoryToConsume;
        }

        @Override
        public String getName() {
            return "Memory Pressure: " + (memoryToConsume / 1024 / 1024) + "MB";
        }

        @Override
        public void inject() throws Exception {
            active = true;
            long consumed = 0;
            while (consumed < memoryToConsume && active) {
                byte[] chunk = new byte[1024 * 1024]; // 1MB chunks
                memoryHogs.add(chunk);
                consumed += chunk.length;
            }
        }

        @Override
        public void recover() throws Exception {
            active = false;
            memoryHogs.clear();
            System.gc(); // Suggest garbage collection
        }

        @Override
        public boolean isRecoverable() {
            return true;
        }
    }

    /**
     * Response time assertion.
     */
    public static class ResponseTimeAssertion implements SystemAssertion {
        private final String endpoint;
        private final Duration maxResponseTime;
        private final Supplier<Duration> responseTimeSupplier;

        public ResponseTimeAssertion(String endpoint, Duration maxResponseTime, 
                                   Supplier<Duration> responseTimeSupplier) {
            this.endpoint = endpoint;
            this.maxResponseTime = maxResponseTime;
            this.responseTimeSupplier = responseTimeSupplier;
        }

        @Override
        public String getName() {
            return "Response Time: " + endpoint + " < " + maxResponseTime.toMillis() + "ms";
        }

        @Override
        public boolean verify() throws Exception {
            Duration actualResponseTime = responseTimeSupplier.get();
            return actualResponseTime.compareTo(maxResponseTime) <= 0;
        }

        @Override
        public String getFailureMessage() {
            return "Response time exceeded " + maxResponseTime.toMillis() + "ms for " + endpoint;
        }
    }

    /**
     * Service availability assertion.
     */
    public static class ServiceAvailabilityAssertion implements SystemAssertion {
        private final String serviceName;
        private final Supplier<Boolean> availabilitySupplier;

        public ServiceAvailabilityAssertion(String serviceName, Supplier<Boolean> availabilitySupplier) {
            this.serviceName = serviceName;
            this.availabilitySupplier = availabilitySupplier;
        }

        @Override
        public String getName() {
            return "Service Available: " + serviceName;
        }

        @Override
        public boolean verify() throws Exception {
            return availabilitySupplier.get();
        }

        @Override
        public String getFailureMessage() {
            return "Service " + serviceName + " is not available";
        }
    }

    /**
     * Data consistency assertion.
     */
    public static class DataConsistencyAssertion implements SystemAssertion {
        private final String description;
        private final Supplier<Boolean> consistencyChecker;

        public DataConsistencyAssertion(String description, Supplier<Boolean> consistencyChecker) {
            this.description = description;
            this.consistencyChecker = consistencyChecker;
        }

        @Override
        public String getName() {
            return "Data Consistency: " + description;
        }

        @Override
        public boolean verify() throws Exception {
            return consistencyChecker.get();
        }

        @Override
        public String getFailureMessage() {
            return "Data consistency violation: " + description;
        }
    }

    /**
     * System health monitor.
     */
    public static class SystemHealthMonitor {
        private final Map<String, HealthMetric> metrics = new ConcurrentHashMap<>();

        public void recordMetric(String name, double value) {
            metrics.computeIfAbsent(name, k -> new HealthMetric(k))
                   .addValue(value);
        }

        public HealthMetric getMetric(String name) {
            return metrics.get(name);
        }

        public Map<String, HealthMetric> getAllMetrics() {
            return new HashMap<>(metrics);
        }

        public void reset() {
            metrics.clear();
        }
    }

    /**
     * Health metric tracking.
     */
    public static class HealthMetric {
        private final String name;
        private final List<Double> values = new ArrayList<>();
        private final List<Instant> timestamps = new ArrayList<>();

        public HealthMetric(String name) {
            this.name = name;
        }

        public synchronized void addValue(double value) {
            values.add(value);
            timestamps.add(Instant.now());
        }

        public synchronized double getAverage() {
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        public synchronized double getMax() {
            return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        }

        public synchronized double getMin() {
            return values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        }

        public synchronized int getCount() {
            return values.size();
        }

        public String getName() { return name; }
        public List<Double> getValues() { return new ArrayList<>(values); }
        public List<Instant> getTimestamps() { return new ArrayList<>(timestamps); }
    }

    /**
     * Experiment result.
     */
    public static class ExperimentResult {
        private final String experimentName;
        private final Instant startTime;
        private final Instant endTime;
        private final boolean successful;
        private final List<AssertionResult> assertionResults;
        private final List<FailureInjectionResult> injectionResults;
        private final Map<String, HealthMetric> healthMetrics;
        private final String errorMessage;

        public ExperimentResult(String experimentName, Instant startTime, Instant endTime,
                               boolean successful, List<AssertionResult> assertionResults,
                               List<FailureInjectionResult> injectionResults,
                               Map<String, HealthMetric> healthMetrics, String errorMessage) {
            this.experimentName = experimentName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.successful = successful;
            this.assertionResults = assertionResults;
            this.injectionResults = injectionResults;
            this.healthMetrics = healthMetrics;
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getExperimentName() { return experimentName; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public Duration getDuration() { return Duration.between(startTime, endTime); }
        public boolean isSuccessful() { return successful; }
        public List<AssertionResult> getAssertionResults() { return assertionResults; }
        public List<FailureInjectionResult> getInjectionResults() { return injectionResults; }
        public Map<String, HealthMetric> getHealthMetrics() { return healthMetrics; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Assertion result.
     */
    public static class AssertionResult {
        private final String assertionName;
        private final boolean passed;
        private final String failureMessage;
        private final Instant timestamp;

        public AssertionResult(String assertionName, boolean passed, String failureMessage) {
            this.assertionName = assertionName;
            this.passed = passed;
            this.failureMessage = failureMessage;
            this.timestamp = Instant.now();
        }

        public String getAssertionName() { return assertionName; }
        public boolean isPassed() { return passed; }
        public String getFailureMessage() { return failureMessage; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Failure injection result.
     */
    public static class FailureInjectionResult {
        private final String injectionName;
        private final boolean successful;
        private final Instant injectionTime;
        private final Instant recoveryTime;
        private final String errorMessage;

        public FailureInjectionResult(String injectionName, boolean successful,
                                     Instant injectionTime, Instant recoveryTime, String errorMessage) {
            this.injectionName = injectionName;
            this.successful = successful;
            this.injectionTime = injectionTime;
            this.recoveryTime = recoveryTime;
            this.errorMessage = errorMessage;
        }

        public String getInjectionName() { return injectionName; }
        public boolean isSuccessful() { return successful; }
        public Instant getInjectionTime() { return injectionTime; }
        public Instant getRecoveryTime() { return recoveryTime; }
        public Duration getInjectionDuration() { 
            return recoveryTime != null ? Duration.between(injectionTime, recoveryTime) : null;
        }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Chaos configuration.
     */
    public static class ChaosConfiguration {
        private int concurrency = 3;
        private Duration defaultTimeout = Duration.ofMinutes(10);
        private boolean safetyMode = true;
        private final Set<String> excludedServices = new HashSet<>();

        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }

        public Duration getDefaultTimeout() { return defaultTimeout; }
        public void setDefaultTimeout(Duration defaultTimeout) { this.defaultTimeout = defaultTimeout; }

        public boolean isSafetyMode() { return safetyMode; }
        public void setSafetyMode(boolean safetyMode) { this.safetyMode = safetyMode; }

        public Set<String> getExcludedServices() { return excludedServices; }
        public void excludeService(String serviceName) { excludedServices.add(serviceName); }
    }

    // Framework methods

    public ChaosExperimentBuilder experiment() {
        return new ChaosExperimentBuilder();
    }

    public void addExperiment(ChaosExperiment experiment) {
        experiments.add(experiment);
    }

    /**
     * Runs a single chaos experiment.
     */
    public CompletableFuture<ExperimentResult> runExperiment(ChaosExperiment experiment) {
        return CompletableFuture.supplyAsync(() -> {
            if (!experiment.isEnabled()) {
                return createSkippedResult(experiment);
            }

            Instant startTime = Instant.now();
            List<AssertionResult> assertionResults = new ArrayList<>();
            List<FailureInjectionResult> injectionResults = new ArrayList<>();
            healthMonitor.reset();

            try {
                // Inject failures
                for (FailureInjection injection : experiment.getFailureInjections()) {
                    Instant injectionStart = Instant.now();
                    try {
                        injection.inject();
                        injectionResults.add(new FailureInjectionResult(
                            injection.getName(), true, injectionStart, null, null));
                    } catch (Exception e) {
                        injectionResults.add(new FailureInjectionResult(
                            injection.getName(), false, injectionStart, null, e.getMessage()));
                    }
                }

                // Run experiment for specified duration
                Thread.sleep(experiment.getDuration().toMillis());

                // Verify system assertions
                for (SystemAssertion assertion : experiment.getAssertions()) {
                    try {
                        boolean passed = assertion.verify();
                        assertionResults.add(new AssertionResult(
                            assertion.getName(), passed, 
                            passed ? null : assertion.getFailureMessage()));
                    } catch (Exception e) {
                        assertionResults.add(new AssertionResult(
                            assertion.getName(), false, e.getMessage()));
                    }
                }

                // Recovery
                for (FailureInjection injection : experiment.getFailureInjections()) {
                    if (injection.isRecoverable()) {
                        try {
                            injection.recover();
                        } catch (Exception e) {
                            // Log recovery failure
                        }
                    }
                }

                Instant endTime = Instant.now();
                boolean successful = assertionResults.stream().allMatch(AssertionResult::isPassed);

                experiment.setResult(new ExperimentResult(
                    experiment.getName(), startTime, endTime, successful,
                    assertionResults, injectionResults, healthMonitor.getAllMetrics(), null));

                return experiment.getResult();

            } catch (Exception e) {
                Instant endTime = Instant.now();
                experiment.setResult(new ExperimentResult(
                    experiment.getName(), startTime, endTime, false,
                    assertionResults, injectionResults, healthMonitor.getAllMetrics(), e.getMessage()));
                return experiment.getResult();
            }
        }, executor);
    }

    /**
     * Runs all experiments.
     */
    public CompletableFuture<List<ExperimentResult>> runAllExperiments() {
        List<CompletableFuture<ExperimentResult>> futures = experiments.stream()
            .map(this::runExperiment)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    private ExperimentResult createSkippedResult(ChaosExperiment experiment) {
        Instant now = Instant.now();
        return new ExperimentResult(
            experiment.getName(), now, now, true,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyMap(), "Experiment skipped (disabled)");
    }

    public SystemHealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}