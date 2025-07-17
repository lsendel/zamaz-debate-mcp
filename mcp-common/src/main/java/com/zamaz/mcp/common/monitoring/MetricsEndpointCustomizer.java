package com.zamaz.mcp.common.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom metrics endpoint for performance monitoring
 */
@Component
@Endpoint(id = "performance-metrics")
@Slf4j
public class MetricsEndpointCustomizer {
    
    /**
     * Get all performance metrics
     */
    @ReadOperation
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // System metrics
            var runtime = Runtime.getRuntime();
            var memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            var osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            var threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
            
            // Memory metrics
            Map<String, Object> memory = new HashMap<>();
            memory.put("total_memory", runtime.totalMemory());
            memory.put("free_memory", runtime.freeMemory());
            memory.put("max_memory", runtime.maxMemory());
            memory.put("used_memory", runtime.totalMemory() - runtime.freeMemory());
            memory.put("heap_used", memoryBean.getHeapMemoryUsage().getUsed());
            memory.put("heap_max", memoryBean.getHeapMemoryUsage().getMax());
            memory.put("heap_committed", memoryBean.getHeapMemoryUsage().getCommitted());
            memory.put("non_heap_used", memoryBean.getNonHeapMemoryUsage().getUsed());
            memory.put("non_heap_max", memoryBean.getNonHeapMemoryUsage().getMax());
            
            // CPU metrics
            Map<String, Object> cpu = new HashMap<>();
            cpu.put("available_processors", osBean.getAvailableProcessors());
            cpu.put("system_load_average", osBean.getSystemLoadAverage());
            cpu.put("process_cpu_load", osBean.getProcessCpuLoad());
            cpu.put("system_cpu_load", osBean.getSystemCpuLoad());
            
            // Thread metrics
            Map<String, Object> threads = new HashMap<>();
            threads.put("thread_count", threadBean.getThreadCount());
            threads.put("daemon_thread_count", threadBean.getDaemonThreadCount());
            threads.put("peak_thread_count", threadBean.getPeakThreadCount());
            threads.put("total_started_thread_count", threadBean.getTotalStartedThreadCount());
            
            // GC metrics
            Map<String, Object> gc = new HashMap<>();
            var gcBeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
            for (var gcBean : gcBeans) {
                Map<String, Object> gcInfo = new HashMap<>();
                gcInfo.put("collection_count", gcBean.getCollectionCount());
                gcInfo.put("collection_time", gcBean.getCollectionTime());
                gc.put(gcBean.getName().replace(" ", "_").toLowerCase(), gcInfo);
            }
            
            // Runtime metrics
            Map<String, Object> runtime_metrics = new HashMap<>();
            var runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            runtime_metrics.put("uptime", runtimeBean.getUptime());
            runtime_metrics.put("start_time", runtimeBean.getStartTime());
            runtime_metrics.put("vm_name", runtimeBean.getVmName());
            runtime_metrics.put("vm_version", runtimeBean.getVmVersion());
            runtime_metrics.put("vm_vendor", runtimeBean.getVmVendor());
            
            // Class loading metrics
            Map<String, Object> classLoading = new HashMap<>();
            var classLoadingBean = java.lang.management.ManagementFactory.getClassLoadingMXBean();
            classLoading.put("loaded_class_count", classLoadingBean.getLoadedClassCount());
            classLoading.put("total_loaded_class_count", classLoadingBean.getTotalLoadedClassCount());
            classLoading.put("unloaded_class_count", classLoadingBean.getUnloadedClassCount());
            
            // Build response
            metrics.put("timestamp", LocalDateTime.now());
            metrics.put("memory", memory);
            metrics.put("cpu", cpu);
            metrics.put("threads", threads);
            metrics.put("gc", gc);
            metrics.put("runtime", runtime_metrics);
            metrics.put("class_loading", classLoading);
            
        } catch (Exception e) {
            log.error("Error collecting performance metrics", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * Get specific metric category
     */
    @ReadOperation
    public Map<String, Object> getMetricCategory(@Selector String category) {
        Map<String, Object> allMetrics = getMetrics();
        
        if (allMetrics.containsKey(category)) {
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", allMetrics.get("timestamp"));
            result.put(category, allMetrics.get(category));
            return result;
        }
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Unknown metric category: " + category);
        error.put("available_categories", allMetrics.keySet());
        return error;
    }
    
    /**
     * Trigger garbage collection
     */
    @WriteOperation
    public Map<String, Object> triggerGc() {
        try {
            long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.currentTimeMillis();
            
            System.gc();
            
            long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", LocalDateTime.now());
            result.put("gc_triggered", true);
            result.put("duration_ms", duration);
            result.put("memory_before", beforeMemory);
            result.put("memory_after", afterMemory);
            result.put("memory_freed", beforeMemory - afterMemory);
            
            log.info("Manual GC triggered - Duration: {}ms, Memory freed: {} bytes", 
                duration, beforeMemory - afterMemory);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error triggering garbage collection", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("timestamp", LocalDateTime.now());
            error.put("gc_triggered", false);
            error.put("error", e.getMessage());
            return error;
        }
    }
    
    /**
     * Get system information
     */
    @ReadOperation
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        
        try {
            // JVM info
            Map<String, Object> jvm = new HashMap<>();
            var runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            jvm.put("name", runtimeBean.getVmName());
            jvm.put("version", runtimeBean.getVmVersion());
            jvm.put("vendor", runtimeBean.getVmVendor());
            jvm.put("spec_name", runtimeBean.getSpecName());
            jvm.put("spec_version", runtimeBean.getSpecVersion());
            jvm.put("spec_vendor", runtimeBean.getSpecVendor());
            jvm.put("management_spec_version", runtimeBean.getManagementSpecVersion());
            
            // System properties
            Map<String, Object> system = new HashMap<>();
            system.put("os_name", System.getProperty("os.name"));
            system.put("os_version", System.getProperty("os.version"));
            system.put("os_arch", System.getProperty("os.arch"));
            system.put("java_version", System.getProperty("java.version"));
            system.put("java_vendor", System.getProperty("java.vendor"));
            system.put("java_home", System.getProperty("java.home"));
            system.put("user_name", System.getProperty("user.name"));
            system.put("user_home", System.getProperty("user.home"));
            system.put("user_dir", System.getProperty("user.dir"));
            system.put("file_separator", System.getProperty("file.separator"));
            system.put("path_separator", System.getProperty("path.separator"));
            system.put("line_separator", System.getProperty("line.separator"));
            
            // Environment variables (filtered)
            Map<String, String> env = new HashMap<>();
            System.getenv().forEach((key, value) -> {
                if (!key.toLowerCase().contains("password") && 
                    !key.toLowerCase().contains("secret") &&
                    !key.toLowerCase().contains("key") &&
                    !key.toLowerCase().contains("token")) {
                    env.put(key, value);
                }
            });
            
            systemInfo.put("timestamp", LocalDateTime.now());
            systemInfo.put("jvm", jvm);
            systemInfo.put("system", system);
            systemInfo.put("environment", env);
            
        } catch (Exception e) {
            log.error("Error collecting system information", e);
            systemInfo.put("error", e.getMessage());
        }
        
        return systemInfo;
    }
    
    /**
     * Get health summary
     */
    @ReadOperation
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            var runtime = Runtime.getRuntime();
            var memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            var osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            var threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
            
            // Memory health
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / maxMemory;
            
            String memoryStatus = memoryUsage > 0.9 ? "CRITICAL" : 
                                 memoryUsage > 0.8 ? "WARNING" : "HEALTHY";
            
            // CPU health
            double cpuLoad = osBean.getProcessCpuLoad();
            String cpuStatus = cpuLoad > 0.9 ? "CRITICAL" : 
                              cpuLoad > 0.7 ? "WARNING" : "HEALTHY";
            
            // Thread health
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            double threadUsage = (double) threadCount / peakThreadCount;
            String threadStatus = threadUsage > 0.9 ? "CRITICAL" : 
                                 threadUsage > 0.8 ? "WARNING" : "HEALTHY";
            
            // Overall health
            String overallStatus = "HEALTHY";
            if ("CRITICAL".equals(memoryStatus) || "CRITICAL".equals(cpuStatus) || "CRITICAL".equals(threadStatus)) {
                overallStatus = "CRITICAL";
            } else if ("WARNING".equals(memoryStatus) || "WARNING".equals(cpuStatus) || "WARNING".equals(threadStatus)) {
                overallStatus = "WARNING";
            }
            
            health.put("timestamp", LocalDateTime.now());
            health.put("overall_status", overallStatus);
            health.put("memory_status", memoryStatus);
            health.put("memory_usage_percent", String.format("%.2f", memoryUsage * 100));
            health.put("cpu_status", cpuStatus);
            health.put("cpu_usage_percent", String.format("%.2f", cpuLoad * 100));
            health.put("thread_status", threadStatus);
            health.put("thread_count", threadCount);
            health.put("uptime_ms", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());
            
        } catch (Exception e) {
            log.error("Error collecting health summary", e);
            health.put("overall_status", "UNKNOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}