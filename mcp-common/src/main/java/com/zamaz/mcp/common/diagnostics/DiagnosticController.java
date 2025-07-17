package com.zamaz.mcp.common.diagnostics;

import com.zamaz.mcp.common.logging.LogContext;
import com.zamaz.mcp.common.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.web.bind.annotation.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diagnostic endpoints for real-time system inspection
 */
@RestController
@RequestMapping("/diagnostics")
@RequiredArgsConstructor
public class DiagnosticController {
    
    private final StructuredLogger logger;
    private final List<HealthIndicator> healthIndicators;
    private final MetricsEndpoint metricsEndpoint;
    private final HttpTraceRepository traceRepository;
    
    /**
     * Get comprehensive system health status
     */
    @GetMapping("/health")
    public Map<String, Object> getHealthStatus() {
        LogContext context = LogContext.forRequest("health-check", "get_health_status");
        
        try {
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("timestamp", Instant.now());
            healthStatus.put("status", "UP");
            healthStatus.put("checks", performHealthChecks());
            healthStatus.put("system", getSystemHealth());
            healthStatus.put("jvm", getJvmHealth());
            healthStatus.put("threads", getThreadHealth());
            healthStatus.put("memory", getMemoryHealth());
            
            logger.info(DiagnosticController.class.getName(), "Health check completed", context);
            return healthStatus;
            
        } catch (Exception e) {
            logger.error(DiagnosticController.class.getName(), "Health check failed", 
                context.exception(e));
            
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("timestamp", Instant.now());
            errorStatus.put("status", "DOWN");
            errorStatus.put("error", e.getMessage());
            return errorStatus;
        }
    }
    
    /**
     * Get detailed JVM metrics
     */
    @GetMapping("/jvm")
    public Map<String, Object> getJvmMetrics() {
        LogContext context = LogContext.forRequest("jvm-metrics", "get_jvm_metrics");
        
        try {
            Map<String, Object> jvmMetrics = new HashMap<>();
            
            // Memory metrics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            jvmMetrics.put("memory", Map.of(
                "heap", memoryBean.getHeapMemoryUsage(),
                "nonHeap", memoryBean.getNonHeapMemoryUsage()
            ));
            
            // Thread metrics
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            jvmMetrics.put("threads", Map.of(
                "count", threadBean.getThreadCount(),
                "daemon", threadBean.getDaemonThreadCount(),
                "peak", threadBean.getPeakThreadCount(),
                "totalStarted", threadBean.getTotalStartedThreadCount()
            ));
            
            // GC metrics
            jvmMetrics.put("gc", getGarbageCollectionMetrics());
            
            // Class loading
            jvmMetrics.put("classLoading", Map.of(
                "loaded", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
                "totalLoaded", ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(),
                "unloaded", ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount()
            ));
            
            logger.info(DiagnosticController.class.getName(), "JVM metrics collected", context);
            return jvmMetrics;
            
        } catch (Exception e) {
            logger.error(DiagnosticController.class.getName(), "Failed to collect JVM metrics", 
                context.exception(e));
            throw new RuntimeException("Failed to collect JVM metrics", e);
        }
    }
    
    /**
     * Get application metrics
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        LogContext context = LogContext.forRequest("app-metrics", "get_metrics");
        
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Get available metric names
            List<String> metricNames = metricsEndpoint.listNames().getNames()
                .stream()
                .sorted()
                .collect(Collectors.toList());
            
            metrics.put("availableMetrics", metricNames);
            
            // Get key metrics
            Map<String, Object> keyMetrics = new HashMap<>();
            for (String metricName : List.of("jvm.memory.used", "jvm.threads.live", 
                "http.server.requests", "system.cpu.usage", "process.uptime")) {
                try {
                    keyMetrics.put(metricName, metricsEndpoint.metric(metricName, null));
                } catch (Exception e) {
                    keyMetrics.put(metricName, "N/A");
                }
            }
            
            metrics.put("keyMetrics", keyMetrics);
            
            logger.info(DiagnosticController.class.getName(), "Application metrics collected", context);
            return metrics;
            
        } catch (Exception e) {
            logger.error(DiagnosticController.class.getName(), "Failed to collect application metrics", 
                context.exception(e));
            throw new RuntimeException("Failed to collect application metrics", e);
        }
    }
    
    /**
     * Get active traces
     */
    @GetMapping("/traces")
    public Map<String, Object> getTraces() {
        LogContext context = LogContext.forRequest("traces", "get_traces");
        
        try {
            Map<String, Object> traces = new HashMap<>();
            traces.put("timestamp", Instant.now());
            traces.put("traces", traceRepository.findAll());
            
            logger.info(DiagnosticController.class.getName(), "Traces collected", context);
            return traces;
            
        } catch (Exception e) {
            logger.error(DiagnosticController.class.getName(), "Failed to collect traces", 
                context.exception(e));
            throw new RuntimeException("Failed to collect traces", e);
        }
    }
    
    /**
     * Get thread dump
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadDump() {
        LogContext context = LogContext.forRequest("thread-dump", "get_thread_dump");
        
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            Map<String, Object> threadDump = new HashMap<>();
            
            threadDump.put("timestamp", Instant.now());
            threadDump.put("threadCount", threadBean.getThreadCount());
            threadDump.put("daemonThreadCount", threadBean.getDaemonThreadCount());
            threadDump.put("peakThreadCount", threadBean.getPeakThreadCount());
            threadDump.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
            
            // Get thread info
            long[] threadIds = threadBean.getAllThreadIds();
            threadDump.put("threads", threadBean.getThreadInfo(threadIds, true, true));
            
            logger.info(DiagnosticController.class.getName(), "Thread dump collected", context);
            return threadDump;
            
        } catch (Exception e) {
            logger.error(DiagnosticController.class.getName(), "Failed to collect thread dump", 
                context.exception(e));
            throw new RuntimeException("Failed to collect thread dump", e);
        }
    }
    
    /**
     * Get configuration dump
     */
    @GetMapping("/config")
    public Map<String, Object> getConfiguration() {
        LogContext context = LogContext.forRequest("config-dump", "get_configuration");
        
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("timestamp", Instant.now());
            config.put("systemProperties", System.getProperties());
            config.put("environmentVariables", System.getenv());
            config.put("jvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
            config.put("classpath", System.getProperty("java.class.path"));
            
            logger.info(DiagnosticController.class.getName(), "Configuration dump collected", context);
            return config;
            
        } catch (Exception e) {
            logger.error(DiagnosticController.class.getName(), "Failed to collect configuration", 
                context.exception(e));
            throw new RuntimeException("Failed to collect configuration", e);
        }
    }
    
    /**
     * Perform health checks
     */
    private Map<String, Object> performHealthChecks() {
        Map<String, Object> checks = new HashMap<>();
        
        for (HealthIndicator indicator : healthIndicators) {
            try {
                Health health = indicator.health();
                checks.put(indicator.getClass().getSimpleName(), Map.of(
                    "status", health.getStatus(),
                    "details", health.getDetails()
                ));
            } catch (Exception e) {
                checks.put(indicator.getClass().getSimpleName(), Map.of(
                    "status", Status.DOWN,
                    "error", e.getMessage()
                ));
            }
        }
        
        return checks;
    }
    
    /**
     * Get system health metrics
     */
    private Map<String, Object> getSystemHealth() {
        Map<String, Object> system = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        system.put("availableProcessors", runtime.availableProcessors());
        system.put("freeMemory", runtime.freeMemory());
        system.put("totalMemory", runtime.totalMemory());
        system.put("maxMemory", runtime.maxMemory());
        
        return system;
    }
    
    /**
     * Get JVM health metrics
     */
    private Map<String, Object> getJvmHealth() {
        Map<String, Object> jvm = new HashMap<>();
        
        jvm.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        jvm.put("startTime", ManagementFactory.getRuntimeMXBean().getStartTime());
        jvm.put("version", System.getProperty("java.version"));
        jvm.put("vendor", System.getProperty("java.vendor"));
        
        return jvm;
    }
    
    /**
     * Get thread health metrics
     */
    private Map<String, Object> getThreadHealth() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new HashMap<>();
        
        threads.put("count", threadBean.getThreadCount());
        threads.put("daemon", threadBean.getDaemonThreadCount());
        threads.put("peak", threadBean.getPeakThreadCount());
        threads.put("totalStarted", threadBean.getTotalStartedThreadCount());
        
        return threads;
    }
    
    /**
     * Get memory health metrics
     */
    private Map<String, Object> getMemoryHealth() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new HashMap<>();
        
        memory.put("heap", memoryBean.getHeapMemoryUsage());
        memory.put("nonHeap", memoryBean.getNonHeapMemoryUsage());
        memory.put("objectPendingFinalization", memoryBean.getObjectPendingFinalizationCount());
        
        return memory;
    }
    
    /**
     * Get garbage collection metrics
     */
    private Map<String, Object> getGarbageCollectionMetrics() {
        Map<String, Object> gc = new HashMap<>();
        
        ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
            gc.put(gcBean.getName(), Map.of(
                "collectionCount", gcBean.getCollectionCount(),
                "collectionTime", gcBean.getCollectionTime()
            ));
        });
        
        return gc;
    }
}