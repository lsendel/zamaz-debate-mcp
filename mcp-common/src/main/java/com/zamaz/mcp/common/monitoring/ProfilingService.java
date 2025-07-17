package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Application profiling service for performance analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilingService {
    
    private final MeterRegistry meterRegistry;
    
    @Value("${monitoring.profiling.enabled:false}")
    private boolean profilingEnabled;
    
    @Value("${monitoring.profiling.sampling.interval:100}")
    private int samplingIntervalMs;
    
    @Value("${monitoring.profiling.cpu.enabled:true}")
    private boolean cpuProfilingEnabled;
    
    @Value("${monitoring.profiling.memory.enabled:true}")
    private boolean memoryProfilingEnabled;
    
    @Value("${monitoring.profiling.thread.enabled:true}")
    private boolean threadProfilingEnabled;
    
    @Value("${monitoring.profiling.method.enabled:false}")
    private boolean methodProfilingEnabled;
    
    @Value("${monitoring.profiling.output.dir:./profiling}")
    private String outputDirectory;
    
    @Value("${monitoring.profiling.retention.days:7}")
    private int retentionDays;
    
    @Value("${monitoring.profiling.max.stack.depth:50}")
    private int maxStackDepth;
    
    @Value("${monitoring.profiling.flame.graph.enabled:false}")
    private boolean flameGraphEnabled;
    
    // Profiling state
    private final AtomicBoolean profilingActive = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, MethodProfile> methodProfiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ThreadProfile> threadProfiles = new ConcurrentHashMap<>();
    private final List<StackSample> stackSamples = Collections.synchronizedList(new ArrayList<>());
    private final List<MemorySnapshot> memorySnapshots = Collections.synchronizedList(new ArrayList<>());
    
    // Thread management
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final java.lang.management.MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * Start profiling session
     */
    public void startProfiling() {
        if (!profilingEnabled) {
            log.warn("Profiling is disabled in configuration");
            return;
        }
        
        if (profilingActive.compareAndSet(false, true)) {
            log.info("Starting profiling session");
            
            // Initialize profiling
            initializeProfiling();
            
            // Record start metrics
            meterRegistry.counter("profiling.sessions.started").increment();
            
            log.info("Profiling session started");
        } else {
            log.warn("Profiling session is already active");
        }
    }
    
    /**
     * Stop profiling session
     */
    public void stopProfiling() {
        if (profilingActive.compareAndSet(true, false)) {
            log.info("Stopping profiling session");
            
            // Generate profiling report
            generateProfilingReport();
            
            // Record stop metrics
            meterRegistry.counter("profiling.sessions.stopped").increment();
            
            log.info("Profiling session stopped");
        } else {
            log.warn("No active profiling session to stop");
        }
    }
    
    /**
     * Check if profiling is active
     */
    public boolean isProfilingActive() {
        return profilingActive.get();
    }
    
    /**
     * Record method execution for profiling
     */
    public void recordMethodExecution(String className, String methodName, long duration, 
                                    long memoryAllocated, int callCount) {
        if (!profilingActive.get() || !methodProfilingEnabled) {
            return;
        }
        
        String key = className + "." + methodName;
        methodProfiles.computeIfAbsent(key, k -> new MethodProfile(className, methodName))
            .record(duration, memoryAllocated, callCount);
    }
    
    /**
     * Record thread activity
     */
    public void recordThreadActivity(String threadName, Thread.State state, long cpuTime, 
                                   long userTime, long blockedTime) {
        if (!profilingActive.get() || !threadProfilingEnabled) {
            return;
        }
        
        threadProfiles.computeIfAbsent(threadName, k -> new ThreadProfile(threadName))
            .record(state, cpuTime, userTime, blockedTime);
    }
    
    /**
     * Take stack sample
     */
    public void takeStackSample() {
        if (!profilingActive.get() || !cpuProfilingEnabled) {
            return;
        }
        
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), maxStackDepth);
        
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null) {
                StackSample sample = new StackSample(
                    threadInfo.getThreadName(),
                    threadInfo.getThreadState(),
                    Arrays.stream(threadInfo.getStackTrace())
                        .limit(maxStackDepth)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.toList()),
                    LocalDateTime.now()
                );
                
                stackSamples.add(sample);
            }
        }
        
        // Limit sample size
        if (stackSamples.size() > 10000) {
            stackSamples.subList(0, stackSamples.size() - 8000).clear();
        }
    }
    
    /**
     * Take memory snapshot
     */
    public void takeMemorySnapshot() {
        if (!profilingActive.get() || !memoryProfilingEnabled) {
            return;
        }
        
        var heapUsage = memoryMXBean.getHeapMemoryUsage();
        var nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        MemorySnapshot snapshot = new MemorySnapshot(
            LocalDateTime.now(),
            heapUsage.getUsed(),
            heapUsage.getMax(),
            heapUsage.getCommitted(),
            nonHeapUsage.getUsed(),
            nonHeapUsage.getMax(),
            nonHeapUsage.getCommitted()
        );
        
        memorySnapshots.add(snapshot);
        
        // Limit snapshot size
        if (memorySnapshots.size() > 1000) {
            memorySnapshots.subList(0, memorySnapshots.size() - 800).clear();
        }
    }
    
    /**
     * Get current profiling summary
     */
    public ProfilingSummary getProfilingSummary() {
        return ProfilingSummary.builder()
            .profilingActive(profilingActive.get())
            .samplingInterval(samplingIntervalMs)
            .methodProfiles(new HashMap<>(methodProfiles))
            .threadProfiles(new HashMap<>(threadProfiles))
            .stackSampleCount(stackSamples.size())
            .memorySnapshotCount(memorySnapshots.size())
            .topMethods(getTopMethods(10))
            .topThreads(getTopThreads(10))
            .build();
    }
    
    /**
     * Get top methods by execution time
     */
    public List<MethodProfile> getTopMethods(int limit) {
        return methodProfiles.values().stream()
            .sorted(Comparator.comparingLong(MethodProfile::getTotalDuration).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get top threads by CPU time
     */
    public List<ThreadProfile> getTopThreads(int limit) {
        return threadProfiles.values().stream()
            .sorted(Comparator.comparingLong(ThreadProfile::getTotalCpuTime).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get method profile
     */
    public MethodProfile getMethodProfile(String className, String methodName) {
        return methodProfiles.get(className + "." + methodName);
    }
    
    /**
     * Get thread profile
     */
    public ThreadProfile getThreadProfile(String threadName) {
        return threadProfiles.get(threadName);
    }
    
    /**
     * Get stack samples for thread
     */
    public List<StackSample> getStackSamples(String threadName, int limit) {
        return stackSamples.stream()
            .filter(sample -> sample.getThreadName().equals(threadName))
            .sorted(Comparator.comparing(StackSample::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get memory snapshots
     */
    public List<MemorySnapshot> getMemorySnapshots(int limit) {
        return memorySnapshots.stream()
            .sorted(Comparator.comparing(MemorySnapshot::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Clear profiling data
     */
    public void clearProfilingData() {
        methodProfiles.clear();
        threadProfiles.clear();
        stackSamples.clear();
        memorySnapshots.clear();
        
        log.info("Profiling data cleared");
    }
    
    /**
     * Export profiling data
     */
    public void exportProfilingData(String format) {
        if (!profilingActive.get() && methodProfiles.isEmpty()) {
            log.warn("No profiling data to export");
            return;
        }
        
        try {
            Path outputPath = Paths.get(outputDirectory);
            Files.createDirectories(outputPath);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("profiling_report_%s.%s", timestamp, format);
            Path reportPath = outputPath.resolve(filename);
            
            switch (format.toLowerCase()) {
                case "json" -> exportToJson(reportPath);
                case "csv" -> exportToCsv(reportPath);
                case "flamegraph" -> exportToFlameGraph(reportPath);
                default -> {
                    log.warn("Unsupported export format: {}", format);
                    return;
                }
            }
            
            log.info("Profiling data exported to: {}", reportPath);
            
        } catch (IOException e) {
            log.error("Failed to export profiling data", e);
        }
    }
    
    /**
     * Sample profiling data periodically
     */
    @Scheduled(fixedRateString = "#{${monitoring.profiling.sampling.interval:100}}")
    public void sampleProfilingData() {
        if (!profilingActive.get()) {
            return;
        }
        
        try {
            // Take stack sample
            if (cpuProfilingEnabled) {
                takeStackSample();
            }
            
            // Take memory snapshot
            if (memoryProfilingEnabled) {
                takeMemorySnapshot();
            }
            
            // Update metrics
            updateProfilingMetrics();
            
        } catch (Exception e) {
            log.error("Error sampling profiling data", e);
        }
    }
    
    /**
     * Clean up old profiling data
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldData() {
        try {
            // Clean up old profiling reports
            Path outputPath = Paths.get(outputDirectory);
            if (Files.exists(outputPath)) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
                
                Files.walk(outputPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant()
                                .isBefore(cutoff.atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete old profiling report: {}", path);
                        }
                    });
            }
            
        } catch (Exception e) {
            log.error("Error cleaning up old profiling data", e);
        }
    }
    
    private void initializeProfiling() {
        // Clear existing data
        clearProfilingData();
        
        // Enable CPU timing if available
        if (threadMXBean.isThreadCpuTimeSupported()) {
            threadMXBean.setThreadCpuTimeEnabled(true);
        }
        
        // Enable contention monitoring if available
        if (threadMXBean.isThreadContentionMonitoringSupported()) {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
        }
        
        log.info("Profiling initialized - CPU: {}, Memory: {}, Threads: {}, Methods: {}", 
            cpuProfilingEnabled, memoryProfilingEnabled, threadProfilingEnabled, methodProfilingEnabled);
    }
    
    private void generateProfilingReport() {
        try {
            ProfilingSummary summary = getProfilingSummary();
            
            log.info("Profiling Report Summary:");
            log.info("- Method profiles: {}", summary.getMethodProfiles().size());
            log.info("- Thread profiles: {}", summary.getThreadProfiles().size());
            log.info("- Stack samples: {}", summary.getStackSampleCount());
            log.info("- Memory snapshots: {}", summary.getMemorySnapshotCount());
            
            // Export to default format
            exportProfilingData("json");
            
        } catch (Exception e) {
            log.error("Error generating profiling report", e);
        }
    }
    
    private void updateProfilingMetrics() {
        meterRegistry.gauge("profiling.method.profiles.count", methodProfiles.size());
        meterRegistry.gauge("profiling.thread.profiles.count", threadProfiles.size());
        meterRegistry.gauge("profiling.stack.samples.count", stackSamples.size());
        meterRegistry.gauge("profiling.memory.snapshots.count", memorySnapshots.size());
    }
    
    private void exportToJson(Path path) throws IOException {
        // In a real implementation, this would use a JSON library
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now()).append("\",\n");
        json.append("  \"methodProfiles\": ").append(methodProfiles.size()).append(",\n");
        json.append("  \"threadProfiles\": ").append(threadProfiles.size()).append(",\n");
        json.append("  \"stackSamples\": ").append(stackSamples.size()).append(",\n");
        json.append("  \"memorySnapshots\": ").append(memorySnapshots.size()).append("\n");
        json.append("}\n");
        
        Files.write(path, json.toString().getBytes());
    }
    
    private void exportToCsv(Path path) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Type,Name,Value,Unit\n");
        
        methodProfiles.forEach((key, profile) -> {
            csv.append("Method,").append(key).append(",").append(profile.getTotalDuration()).append(",ms\n");
        });
        
        threadProfiles.forEach((key, profile) -> {
            csv.append("Thread,").append(key).append(",").append(profile.getTotalCpuTime()).append(",ms\n");
        });
        
        Files.write(path, csv.toString().getBytes());
    }
    
    private void exportToFlameGraph(Path path) throws IOException {
        if (!flameGraphEnabled) {
            log.warn("Flame graph export is not enabled");
            return;
        }
        
        // In a real implementation, this would generate flame graph data
        StringBuilder flameGraph = new StringBuilder();
        flameGraph.append("# Flame Graph Data\n");
        flameGraph.append("# Generated at: ").append(LocalDateTime.now()).append("\n");
        
        Files.write(path, flameGraph.toString().getBytes());
    }
    
    // Data classes
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MethodProfile {
        private String className;
        private String methodName;
        private long totalDuration = 0;
        private long callCount = 0;
        private long totalMemoryAllocated = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;
        
        public MethodProfile(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
        
        public synchronized void record(long duration, long memoryAllocated, int calls) {
            totalDuration += duration;
            callCount += calls;
            totalMemoryAllocated += memoryAllocated;
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
        }
        
        public synchronized double getAverageDuration() {
            return callCount > 0 ? (double) totalDuration / callCount : 0.0;
        }
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ThreadProfile {
        private String threadName;
        private long totalCpuTime = 0;
        private long totalUserTime = 0;
        private long totalBlockedTime = 0;
        private long sampleCount = 0;
        private final Map<Thread.State, Long> stateCount = new ConcurrentHashMap<>();
        
        public ThreadProfile(String threadName) {
            this.threadName = threadName;
        }
        
        public synchronized void record(Thread.State state, long cpuTime, long userTime, long blockedTime) {
            totalCpuTime += cpuTime;
            totalUserTime += userTime;
            totalBlockedTime += blockedTime;
            sampleCount++;
            stateCount.merge(state, 1L, Long::sum);
        }
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class StackSample {
        private String threadName;
        private Thread.State threadState;
        private List<String> stackTrace;
        private LocalDateTime timestamp;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MemorySnapshot {
        private LocalDateTime timestamp;
        private long heapUsed;
        private long heapMax;
        private long heapCommitted;
        private long nonHeapUsed;
        private long nonHeapMax;
        private long nonHeapCommitted;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProfilingSummary {
        private boolean profilingActive;
        private int samplingInterval;
        private Map<String, MethodProfile> methodProfiles;
        private Map<String, ThreadProfile> threadProfiles;
        private int stackSampleCount;
        private int memorySnapshotCount;
        private List<MethodProfile> topMethods;
        private List<ThreadProfile> topThreads;
    }
}